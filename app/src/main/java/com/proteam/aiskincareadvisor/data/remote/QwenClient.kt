package com.proteam.aiskincareadvisor.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class QwenClient(
    private val apiKey: String,
    private val baseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1"
) {
    private val http = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun chat(
        model: String = "qwen-max",
        system: String,
        user: String
    ): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
            .put(JSONObject().put("role", "user").put("content", user))

        val bodyJson = JSONObject()
            .put("model", model)
            .put("messages", messages)

        val req = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RuntimeException("Qwen request failed: HTTP ${resp.code}\n$raw")
            }
            val root = JSONObject(raw)
            val choices = root.getJSONArray("choices")
            val content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            return content
        }
    }
}