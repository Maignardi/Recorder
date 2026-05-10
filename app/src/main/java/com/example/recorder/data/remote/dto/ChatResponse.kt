package com.example.recorder.data.remote.dto

data class ChatResponse(val choices: List<ChatChoice>)
data class ChatChoice(val message: ChatMessage)
