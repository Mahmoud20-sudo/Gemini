package com.me.gemini.helper

// VoiceChatHandler.kt
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.util.Locale

class VoiceChatHandler(
    private val context: Context,
    private val onTextReceived: (String) -> Unit,
    private val onError: (String) -> Unit
) : LifecycleObserver, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isListening: Boolean = false

    init {
        (context as? ComponentActivity)?.lifecycle?.addObserver(this)
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context, this)
    }

    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context).not()) {
            onError("Voice recognition not available")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA") // Arabic (Saudi Arabia)
             putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG") // Arabic (Egypt)
             putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...") // Optional prompt
        }

        speechRecognizer?.startListening(intent)
    }

    fun speak(text: String) {
        if (isTtsInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun cleanup() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }

    override fun onInit(status: Int) {
        isTtsInitialized = status == TextToSpeech.SUCCESS
        if (isTtsInitialized) {
            tts?.language = Locale.getDefault()
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onResults(results: Bundle) {
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { onTextReceived(it) }
            isListening = false
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Handle partial recognition results if needed
            // partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Handle events if needed
        }

        override fun onReadyForSpeech(params: Bundle?) {
            // Called when the recognizer is ready to receive speech
            isListening = true
        }

        override fun onBeginningOfSpeech() {
            // Called when the user starts speaking
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Called when the sound level in the audio stream changes
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Called when audio buffer is received (not commonly used)
        }

        override fun onEndOfSpeech() {
            // Called when the user stops speaking
        }

        override fun onError(error: Int) {
            onError("Voice recognition error: $error")
            isListening = false
        }
    }
}