package com.example.recorder.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024
)

data class ChatMessage(val role: String, val content: String)
