package com.hasseena.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.hasseena.assistant.ai.AiAction
import java.util.Locale

class ActionExecutor(private val context: Context) {

    fun execute(action: AiAction): String = execute(action.type, action.value)

    fun execute(type: String, value: String? = null): String {
        return try {
            when (type) {
                "OPEN_APP" -> openApp(value)
                "YOUTUBE" -> openPackage("com.google.android.youtube", "YouTube")
                "WHATSAPP" -> openPackage("com.whatsapp", "WhatsApp")
                "CAMERA" -> {
                    val intent = Intent("android.media.action.IMAGE_CAPTURE")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    "Camera khol rahi hoon."
                }
                "WEB_SEARCH" -> {
                    val query = value?.ifBlank { "Google" } ?: "Google"
                    start(Intent(Intent.ACTION_VIEW, Uri.parse(
                        "https://www.google.com/search?q=${Uri.encode(query)}"
                    )))
                    "Search kar rahi hoon."
                }
                "OPEN_WEBSITE" -> {
                    start(Intent(Intent.ACTION_VIEW, Uri.parse(value ?: "https://www.google.com")))
                    "Website khol rahi hoon."
                }
                "DIAL" -> {
                    val number = value?.trim()
                    val uri = if (!number.isNullOrBlank()) "tel:$number" else "tel:"
                    start(Intent(Intent.ACTION_DIAL, Uri.parse(uri)))
                    if (number.isNullOrBlank()) "Phone app khol rahi hoon." else "Dialer khol rahi hoon."
                }
                "SETTINGS" -> openSettings(Settings.ACTION_SETTINGS, "Settings")
                "WIFI_SETTINGS" -> openSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi settings")
                "BLUETOOTH_SETTINGS" -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth settings")
                "APP_SETTINGS" -> {
                    start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")))
                    "App settings khol rahi hoon."
                }
                else -> "Ji, command samajh gayi."
            }
        } catch (_: Exception) {
            "Ye action abhi perform nahi ho saka."
        }
    }

    private fun openApp(nameOrPackage: String?): String {
        if (nameOrPackage.isNullOrBlank()) return "App ka naam nahi mila."
        val packageIntent = context.packageManager.getLaunchIntentForPackage(nameOrPackage)
        if (packageIntent != null) {
            start(packageIntent)
            return "App khol rahi hoon."
        }

        val wanted = nameOrPackage.lowercase(Locale.getDefault()).trim()
        val packages = context.packageManager.getInstalledApplications(0)
        val match = packages.firstOrNull {
            context.packageManager.getApplicationLabel(it).toString()
                .lowercase(Locale.getDefault()).contains(wanted)
        }
        if (match != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(match.packageName)
            if (intent != null) {
                start(intent)
                return "${context.packageManager.getApplicationLabel(match)} khol rahi hoon."
            }
        }

        start(Intent(Intent.ACTION_VIEW, Uri.parse(
            "https://www.google.com/search?q=${Uri.encode(nameOrPackage)}"
        )))
        return "App phone mein nahi mili, search khol rahi hoon."
    }

    private fun openPackage(pkg: String, label: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            start(intent)
            return "$label khol rahi hoon."
        }
        return openApp(label)
    }

    private fun openSettings(action: String, label: String): String {
        start(Intent(action))
        return "$label khol rahi hoon."
    }

    private fun start(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
