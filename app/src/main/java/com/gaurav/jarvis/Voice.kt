package com.gaurav.jarvis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Speech-to-text using Android's built-in recognizer. */
class Voice(private val ctx: Context, private val cb: Callback) {
    interface Callback {
        fun onListening()
        fun onRms(level: Float)
        fun onResult(text: String)
        fun onFailure(message: String, silent: Boolean, fatal: Boolean)
    }

    private var rec: SpeechRecognizer? = null

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(ctx)

    fun start(lang: String) {
        stop()
        val r = SpeechRecognizer.createSpeechRecognizer(ctx)
        rec = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { cb.onListening() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { cb.onRms(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text.isNullOrBlank()) cb.onFailure("I didn't catch that.", true, false) else cb.onResult(text)
            }
            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        cb.onFailure("I didn't catch that.", true, false)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT ->
                        cb.onFailure("", true, false)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        cb.onFailure("Microphone permission is missing.", false, true)
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
                        cb.onFailure("Speech recognition could not reach the network. Check your connection.", false, false)
                    else -> cb.onFailure("Speech recognition error $error.", false, false)
                }
            }
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (lang == "ne") "ne-NP" else "en-US")
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        r.startListening(i)
    }

    fun stop() {
        rec?.cancel()
        rec?.destroy()
        rec = null
    }
}
