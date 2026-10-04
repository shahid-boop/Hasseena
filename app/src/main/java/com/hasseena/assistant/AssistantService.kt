package com.hasseena.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.hasseena.assistant.ai.AiBrain
import java.util.Locale

class AssistantService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private val aiBrain = AiBrain()
    private lateinit var actionExecutor: ActionExecutor

    private val channelId = "hasseena_assistant"
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            notificationId,
            buildNotification("Hasseena background assistant active")
        )

        actionExecutor = ActionExecutor(this)

        setupTTS()
        setupSpeechRecognizer()

        startListening()
    }

    private fun setupTTS() {
        textToSpeech = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                textToSpeech?.setLanguage(Locale.US)
                textToSpeech?.setPitch(1.0f)
                textToSpeech?.setSpeechRate(0.90f)
            }
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("Speech recognition unavailable")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                updateNotification("Hasseena listening...")
            }

            override fun onBeginningOfSpeech() {
                updateNotification("Hasseena listening...")
            }

            override fun onResults(results: Bundle?) {
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()

                if (spokenText.isNotBlank()) {
                    processCommand(spokenText)
                } else {
                    restartListening()
                }
            }

            override fun onError(error: Int) {
                restartListening()
            }

            override fun onEndOfSpeech() {
                // Results/error will restart listening.
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun processCommand(spokenText: String) {
        val result = aiBrain.process(spokenText)

        updateNotification("Hasseena: ${result.reply}")

        if (result.actions.isNotEmpty()) {
            val action = result.actions.first()
            val message = actionExecutor.execute(action.type, action.value)
            updateNotification("Hasseena: $message")
        }

        if (result.shouldSpeak) {
            speak(result.reply)
        } else {
            restartListening()
        }
    }

    private fun speak(text: String) {
        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "HASSEENA"
        )

        // Give TTS a moment before starting the next recognition cycle.
        android.os.Handler(mainLooper).postDelayed(
            { restartListening() },
            1800L
        )
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
            )
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            restartListening()
        }
    }

    private fun restartListening() {
        android.os.Handler(mainLooper).postDelayed(
            { startListening() },
            500L
        )
    }

    private fun createNotificationChannel() {
        val manager =
            getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            channelId,
            "Hasseena Assistant",
            NotificationManager.IMPORTANCE_LOW
        )

        channel.description =
            "Hasseena background voice assistant"

        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Hasseena")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager =
            getSystemService(NotificationManager::class.java)

        manager.notify(
            notificationId,
            buildNotification(text)
        )
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
