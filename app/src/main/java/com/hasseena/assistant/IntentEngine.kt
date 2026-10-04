package com.hasseena.assistant

data class IntentResult(val type: String, val originalText: String)

class IntentEngine {
    fun understand(input: String): IntentResult {
        val text = input.trim().lowercase()
        val type = when {
            text.isBlank() -> "EMPTY"
            text.contains("salam") || text.contains("hello") || text.contains("hi") -> "GREETING"
            text.contains("time") || text.contains("baje") -> "TIME"
            text.contains("youtube") -> "YOUTUBE"
            text.contains("whatsapp") -> "WHATSAPP"
            text.contains("camera") || text.contains("kamyra") -> "CAMERA"
            text.contains("weather") || text.contains("mausam") -> "WEATHER"
            text.contains("search") || text.contains("google") -> "WEB_SEARCH"
            else -> "GENERAL"
        }
        return IntentResult(type, input)
    }
}
