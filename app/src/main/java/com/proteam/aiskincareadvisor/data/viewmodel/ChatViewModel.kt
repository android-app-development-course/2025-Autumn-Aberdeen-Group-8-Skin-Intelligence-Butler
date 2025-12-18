package com.proteam.aiskincareadvisor.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proteam.aiskincareadvisor.BuildConfig
import com.proteam.aiskincareadvisor.data.remote.QwenClient
import com.proteam.aiskincareadvisor.ui.screens.main.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                text = "Hello! I'm your AI skincare assistant. How can I help you today?",
                isFromUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // 先把用户消息加进去
        _messages.value = _messages.value + ChatMessage(text = trimmed, isFromUser = true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val replyText = withContext(Dispatchers.IO) {
                    val apiKey = BuildConfig.DASHSCOPE_API_KEY
                    if (apiKey.isBlank()) {
                        throw IllegalStateException("DASHSCOPE_API_KEY is empty. Please set it in local.properties/gradle properties.")
                    }

                    val client = QwenClient(apiKey = apiKey)
                    client.chat(
                        model = "qwen-max",
                        system = "You are a skincare analysis expert. Only answer skincare-related questions. Reply in English.",
                        user = trimmed
                    )
                }

                _messages.value = _messages.value + ChatMessage(text = replyText, isFromUser = false)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    text = "Error: ${e.message ?: e.javaClass.simpleName}",
                    isFromUser = false
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}