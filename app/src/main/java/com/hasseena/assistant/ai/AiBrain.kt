package com.hasseena.assistant.ai

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiBrain {

    fun process(input: String): AiResponse {
        val original = input.trim()
        val text = original.lowercase(Locale.getDefault())

        if (text.isBlank()) return AiResponse("Ji, boliye. Main sun rahi hoon.")

        if (text.contains("salam") || text.contains("assalam") ||
            text == "hello" || text == "hi" || text.contains("helo")) {
            return AiResponse("Wa Alaikum Assalam. Main Hasseena hoon. Bataiye, kya karun?")
        }

        if (text.contains("tumhara naam") || text.contains("ap ka naam") ||
            text.contains("your name") || text.contains("who are you")) {
            return AiResponse("Mera naam Hasseena hai.")
        }

        if (text.contains("time") || text.contains("kitne baje") ||
            text.contains("kitna baj") || text.contains("waqt kya") ||
            text.contains("current time")) {
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            return AiResponse("Abhi $time baj rahe hain.")
        }

        if (text.contains("youtube") || text.contains("you tube"))
            return action("YouTube khol rahi hoon.", "YOUTUBE")

        if (text.contains("whatsapp") || text.contains("what's app"))
            return action("WhatsApp khol rahi hoon.", "WHATSAPP")

        if (text.contains("camera") || text.contains("kamyra"))
            return action("Camera khol rahi hoon.", "CAMERA")

        if (text.contains("wifi") || text.contains("wi-fi"))
            return action("Wi-Fi settings khol rahi hoon.", "WIFI_SETTINGS")

        if (text.contains("bluetooth"))
            return action("Bluetooth settings khol rahi hoon.", "BLUETOOTH_SETTINGS")

        if (text.contains("settings") || text.contains("setting kholo") || text.contains("setting khol"))
            return action("Settings khol rahi hoon.", "SETTINGS")

        if (text.contains("dialer") || text.contains("phone kholo") ||
            text.contains("call app kholo") || text.contains("phone app")) {
            val number = extractPhoneNumber(original)
            return if (number != null)
                action("Dialer mein number khol rahi hoon.", "DIAL", number)
            else
                action("Phone app khol rahi hoon.", "DIAL")
        }

        // Explicit common-app requests. OPEN_APP can also resolve installed apps by label.
        val app = extractAppName(text)
        if (app != null) {
            return action("$app khol rahi hoon.", "OPEN_APP", app)
        }

        if (text.startsWith("search ") || text.startsWith("google ") ||
            text.contains("search karo") || text.contains("google par")) {
            return action("Search kar rahi hoon.", "WEB_SEARCH", extractSearchQuery(original))
        }

        if (text.contains("weather") || text.contains("mausam")) {
            return action("Mausam check kar rahi hoon.", "WEB_SEARCH", "weather today")
        }

        if (text.contains("google kholo"))
            return action("Google khol rahi hoon.", "OPEN_WEBSITE", "https://www.google.com")

        return AiResponse(
            "Ji, main sun rahi hoon. Aap app kholne, search karne, time batane, camera, phone ya settings ka keh sakte hain."
        )
    }

    private fun action(reply: String, type: String, value: String? = null) =
        AiResponse(reply = reply, actions = listOf(AiAction(type, value)))

    private fun extractPhoneNumber(input: String): String? {
        val match = Regex("""(?:\+?92|0)\s*\d(?:[\s-]*\d){9,10}""").find(input)
        return match?.value?.replace(Regex("""[\s-]"""), "")
    }

    private fun extractAppName(text: String): String? {
        val known = listOf(
            "chrome", "google maps", "maps", "facebook", "instagram",
            "telegram", "tiktok", "spotify", "gmail", "messages",
            "calculator", "gallery", "clock", "contacts", "files"
        )
        val openWords = listOf("open", "khol", "kholo", "chalao", "launch", "start")
        if (!openWords.any { text.contains(it) }) return null
        return known.firstOrNull { text.contains(it) }
    }

    private fun extractSearchQuery(input: String): String {
        val lower = input.lowercase(Locale.getDefault())
        val prefixes = listOf("search karo", "google par", "search", "google")
        for (prefix in prefixes) {
            val index = lower.indexOf(prefix)
            if (index >= 0) {
                val result = input.substring(index + prefix.length).trim()
                if (result.isNotBlank()) return result
            }
        }
        return input
    }
}
