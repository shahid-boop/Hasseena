package com.hasseena.assistant.ai

data class AiResponse(
    val reply: String,
    val actions: List<AiAction> = emptyList(),
    val shouldSpeak: Boolean = true
)
