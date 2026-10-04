package com.hasseena.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.hasseena.assistant.ai.AiBrain
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var listenButton: Button
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech

    private val aiBrain = AiBrain()
    private lateinit var actionExecutor: ActionExecutor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionExecutor = ActionExecutor(this)

        buildUI()
        setupTTS()
        setupSpeechRecognizer()

        if (
            checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        }
    }

    private fun buildUI() {

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(40, 40, 40, 40)

        statusText = TextView(this)
        statusText.text = "Hasseena ready"
        statusText.textSize = 22f
        statusText.gravity = Gravity.CENTER

        listenButton = Button(this)
        listenButton.text = "🎙  Listen"

        layout.addView(statusText)
        layout.addView(listenButton)

        setContentView(layout)

        listenButton.setOnClickListener {
            startListening()
        }
    }

    private fun setupTTS() {

        textToSpeech = TextToSpeech(this) { result ->

            if (result != TextToSpeech.SUCCESS) {
                return@TextToSpeech
            }

            setupMatureVoice()
        }
    }

    private fun setupMatureVoice() {

        val voices = textToSpeech.voices ?: emptySet()

        val preferred =
            voices
                .filter {
                    it.locale.language == Locale.US.language &&
                    !it.isNetworkConnectionRequired
                }
                .sortedBy { voice ->
                    val name = voice.name.lowercase()

                    when {
                        name.contains("female") -> 0
                        name.contains("woman") -> 1
                        name.contains("zira") -> 2
                        name.contains("samantha") -> 3
                        else -> 10
                    }
                }
                .firstOrNull()

        if (preferred != null) {
            textToSpeech.voice = preferred
        } else {
            textToSpeech.setLanguage(Locale.US)
        }

        // Generic mature / calm / confident style.
        // This does not imitate any real person.
        textToSpeech.setPitch(0.84f)
        textToSpeech.setSpeechRate(0.88f)
    }

    private fun setupSpeechRecognizer() {

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    statusText.text = "🎙 Listening..."
                }

                override fun onBeginningOfSpeech() {
                    statusText.text = "Bolain..."
                }

                override fun onResults(results: Bundle?) {

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val spokenText =
                        matches?.firstOrNull().orEmpty()

                    if (spokenText.isBlank()) {
                        statusText.text = "Samajh nahi aaya."
                        return
                    }

                    processCommand(spokenText)
                }

                override fun onError(error: Int) {
                    statusText.text =
                        "Voice ready — dobara bolain."
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )
    }

    private fun processCommand(spokenText: String) {

        val result = aiBrain.process(spokenText)

        statusText.text =
            "$spokenText\n\nHasseena: ${result.reply}"

        if (result.actions.isNotEmpty()) {

            val action = result.actions.first()

            val message =
                actionExecutor.execute(
                    action.type,
                    action.value
                )

            speak(message)

        } else if (result.shouldSpeak) {

            speak(result.reply)
        }
    }

    private fun startListening() {

        if (
            checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
            return
        }

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        // Android can choose the best available language.
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_PARTIAL_RESULTS,
            false
        )

        try {
            speechRecognizer.startListening(intent)
        } catch (_: Exception) {
            statusText.text =
                "Voice recognition start nahi ho saki."
        }
    }

    private fun speak(text: String) {

        textToSpeech.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "HASSEENA"
        )
    }

    override fun onDestroy() {

        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        super.onDestroy()
    }
}
