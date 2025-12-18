package com.proteam.aiskincareadvisor.data.repository

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.proteam.aiskincareadvisor.data.firestore.SkinAnalysisStorage
import com.proteam.aiskincareadvisor.data.model.Product
import com.proteam.aiskincareadvisor.data.model.SkinAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class SkinAnalysisRepository(private val context: Context) {

    // Your Hong Kong HTTPS proxy (nginx -> qwen-proxy -> DashScope)
    private val PROXY_BASE_URL = "https://8.217.74.211.nip.io"
    private val CHAT_ENDPOINT = "$PROXY_BASE_URL/chat/completions"

    /**
     * Use a model that your DashScope account has access to.
     * - If you ONLY have text model access: keep "qwen-max" (image may be ignored or may error).
     * - If you have vision model access: switch to the proper Qwen-VL model name here.
     */
    private val MODEL = "qwen-max"

    suspend fun analyzeSkinImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1) Prepare image as Base64 Data URL (keep payload small: proxy has JSON limit)
            val imageBase64 = compressImageToBase64(
                imageUri = imageUri,
                maxDimension = 1024,
                jpegQuality = 80
            )
            val imageDataUrl = "data:image/jpeg;base64,$imageBase64"

            // 2) Fetch data from Firestore (safe fallback if permissions are not ready)
            val availableSkinTypes = fetchAvailableSkinTypesSafe()
            val productListText = fetchProductListForAISafe()

            // 3) Build OpenAI-compatible messages (multimodal "content")
            val systemPrompt =
                "You are a professional skincare analyst. Reply in English in a friendly, clear style, strictly following the required output format."

            val userPrompt = buildString {
                appendLine("Analyze this skin image.")
                appendLine("Provide: skin type, hydration level, oil level, overall condition, and skin concerns.")
                appendLine("Then provide 3–5 skincare recommendations and 1–3 helpful tips.")
                appendLine("Classify skin type as one of: ${availableSkinTypes.joinToString(", ")}.")
                appendLine("Below is the list of available products in our database (name, category, suitable skin types):")
                appendLine(productListText)
                appendLine("Based on the user's skin type, pick 1–2 best matching products from the list.")
                appendLine("Output strictly in the format below (DO NOT use curly braces or quotes):")
                appendLine("skinType: ...")
                appendLine("hydrationLevel: ...")
                appendLine("oilLevel: ...")
                appendLine("overallCondition: ...")
                appendLine("concerns: [ ... ]")
                appendLine("recommendations: [ ... ]")
                appendLine("tips: [ ... ]")
                appendLine("recommendedProducts: [ ... ]")
            }

            val userContent = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", userPrompt)
                })
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", imageDataUrl)
                    })
                })
            }

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("stream", false)
                put("messages", messages)
            }

            // 4) Call your proxy over HTTPS (no DashScope key in Android)
            val rawJson = httpPostJson(CHAT_ENDPOINT, requestBody.toString())
            Log.d(TAG, "AI Raw Response: $rawJson")

            // 5) Extract assistant content: choices[0].message.content
            val assistantText = extractAssistantContent(rawJson)
                ?: throw IllegalStateException(extractErrorMessage(rawJson) ?: "Empty AI response")

            // 6) Parse & store (storage may fail if Firestore rules not ready; do not crash)
            val parsed = parseTextToSkinResult(assistantText)
            Log.d(TAG, "Parsed Result: $parsed")

            if (parsed != null) {
                try {
                    SkinAnalysisStorage().saveAnalysisResult(parsed)
                } catch (e: Exception) {
                    Log.w(TAG, "Saving analysis result failed (Firestore rules/auth?): ${e.message}")
                }
            }

            // 7) Presentable format
            val displayText = formatTextForDisplay(assistantText)
            Result.success(displayText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------
    // HTTP helpers
    // ----------------------------

    private fun httpPostJson(url: String, jsonBody: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        conn.outputStream.use { os ->
            os.write(jsonBody.toByteArray(Charsets.UTF_8))
        }

        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }

        if (conn.responseCode !in 200..299) {
            // Bubble up server error (e.g., model not supported / invalid request)
            throw IllegalStateException("HTTP ${conn.responseCode}: $body")
        }
        return body
    }

    private fun extractAssistantContent(rawJson: String): String? {
        return try {
            val obj = JSONObject(rawJson)
            val choices = obj.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val msg = choices.getJSONObject(0).optJSONObject("message") ?: return null
            msg.optString("content", null)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractErrorMessage(rawJson: String): String? {
        return try {
            val obj = JSONObject(rawJson)
            val err = obj.optJSONObject("error") ?: return null
            err.optString("message", null)
        } catch (_: Exception) {
            null
        }
    }

    // ----------------------------
    // Image -> Base64 (keep request small)
    // ----------------------------

    private fun compressImageToBase64(
        imageUri: Uri,
        maxDimension: Int,
        jpegQuality: Int
    ): String {
        // Read bounds
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(imageUri).use { ins ->
            if (ins == null) throw IllegalArgumentException("Unable to open image input stream")
            BitmapFactory.decodeStream(ins, null, boundsOpts)
        }

        val w = boundsOpts.outWidth
        val h = boundsOpts.outHeight
        if (w <= 0 || h <= 0) throw IllegalArgumentException("Invalid image")

        val sampleSize = computeInSampleSize(w, h, maxDimension, maxDimension)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }

        val bmp: Bitmap = context.contentResolver.openInputStream(imageUri).use { ins ->
            if (ins == null) throw IllegalArgumentException("Unable to reopen image input stream")
            BitmapFactory.decodeStream(ins, null, decodeOpts)
        } ?: throw IllegalArgumentException("Decode bitmap failed")

        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun computeInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while ((halfWidth / inSampleSize) >= reqWidth && (halfHeight / inSampleSize) >= reqHeight) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    // ----------------------------
    // Firestore (safe fallbacks)
    // ----------------------------

    private suspend fun fetchProductListForAISafe(): String {
        return try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("products").get().await()
            snapshot.documents.joinToString("\n") { doc ->
                val name = doc.getString("name") ?: "Unnamed"
                val category = doc.getString("category") ?: "Unknown category"
                val skinTypes = (doc["skinTypes"] as? List<*>)?.joinToString(", ") ?: "Unknown skin types"
                "- Name: $name | Category: $category | Suitable for: $skinTypes"
            }.ifBlank { "No products found." }
        } catch (e: Exception) {
            Log.w(TAG, "Fetching products failed (Firestore rules/auth?): ${e.message}")
            "Product list unavailable (database permission not ready)."
        }
    }

    private suspend fun fetchAvailableSkinTypesSafe(): List<String> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("products").get().await()
            val skinTypes = mutableSetOf<String>()
            snapshot.documents.forEach { doc ->
                val product = doc.toObject(Product::class.java)
                product?.skinTypes?.forEach { skinTypes.add(it) }
            }
            if (skinTypes.isEmpty()) listOf("Dry", "Oily", "Combination", "Normal", "Sensitive")
            else skinTypes.toList()
        } catch (e: Exception) {
            Log.w(TAG, "Fetching skin types failed (Firestore rules/auth?): ${e.message}")
            listOf("Dry", "Oily", "Combination", "Normal", "Sensitive")
        }
    }

    // ----------------------------
    // Parsing & display formatting
    // ----------------------------

    suspend fun parseTextToSkinResult(text: String): SkinAnalysisResult? {
        fun extractList(key: String): List<String> {
            val regex = Regex("""$key:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
            val raw = regex.find(text)?.groupValues?.get(1) ?: return emptyList()
            return raw.split(",").map { it.trim().removePrefix("\"").removeSuffix("\"") }
        }

        fun extractField(key: String): String {
            val regex = Regex("""$key:\s*(.+)""")
            return regex.find(text)?.groupValues?.get(1)?.trim() ?: ""
        }

        val recommendedProducts = extractList("recommendedProducts")

        val matchingProductIds = findMatchingProducts(
            extractField("skinType"),
            recommendedProducts
        )

        return SkinAnalysisResult(
            skinType = extractField("skinType"),
            hydrationLevel = extractField("hydrationLevel"),
            oilLevel = extractField("oilLevel"),
            overallCondition = extractField("overallCondition"),
            concerns = extractList("concerns"),
            recommendations = extractList("recommendations"),
            tips = extractList("tips"),
            recommendedProductIds = matchingProductIds
        )
    }

    private suspend fun findMatchingProducts(skinType: String, productNames: List<String>): List<String> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val matchingIds = mutableListOf<String>()

            for (productName in productNames) {
                val query = db.collection("products")
                    .whereEqualTo("name", productName)
                    .whereArrayContains("skinTypes", skinType)
                    .limit(1)

                val results = query.get().await()
                matchingIds.addAll(results.documents.mapNotNull { it.id })
            }

            matchingIds.take(5)
        } catch (e: Exception) {
            Log.w(TAG, "Matching products failed: ${e.message}")
            emptyList()
        }
    }

    fun formatTextForDisplay(text: String): String {
        fun extractList(key: String): List<String> {
            val regex = Regex("""$key:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(text) ?: return emptyList()
            return match.groupValues[1]
                .split(",")
                .map { it.trim().removeSurrounding("\"").removePrefix("-").trim() }
                .filter { it.isNotEmpty() }
        }

        fun extractField(key: String): String {
            val regex = Regex("""$key:\s*(.+?)(\n|$)""", RegexOption.DOT_MATCHES_ALL)
            return regex.find(text)?.groupValues?.get(1)?.trim() ?: ""
        }

        return buildString {
            appendLine("• **Skin type**: ${extractField("skinType")}")
            appendLine("• **Hydration level**: ${extractField("hydrationLevel")}")
            appendLine("• **Oil level**: ${extractField("oilLevel")}")
            appendLine("• **Overall condition**: ${extractField("overallCondition")}")

            val concerns = extractList("concerns")
            if (concerns.isNotEmpty()) {
                appendLine("\n• **Concerns:**")
                concerns.forEach { appendLine("- $it") }
            }

            val recommendations = extractList("recommendations")
            if (recommendations.isNotEmpty()) {
                appendLine("\n• **Recommendations:**")
                recommendations.forEach { appendLine("- $it") }
            }

            val tips = extractList("tips")
            if (tips.isNotEmpty()) {
                appendLine("\n• **Tips:**")
                tips.forEach { appendLine("- $it") }
            }

            val products = extractList("recommendedProducts")
            if (products.isNotEmpty()) {
                appendLine("\n• **Recommended products:**")
                products.forEach { appendLine("- $it") }
            }
        }.trim()
    }

    // Keep this if other parts of your app still need a temp file.
    private fun saveImageToTempFile(imageUri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw IllegalArgumentException("Unable to open image stream")
        val tempFile = File.createTempFile("skin_image", ".jpg", context.cacheDir)
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        return tempFile
    }
}