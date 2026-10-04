package com.hasseena.assistant.ai

data class AiAction(
    val type: String,
    val value: String? = null,
    val requiresConfirmation: Boolean = false
)
