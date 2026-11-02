package com.gaurav.jarvis

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Text-to-speech wrapper. onState(true) while speaking, onState(false) when finished or stopped. */
class Speaker(ctx: Context, private val onState: (Boolean) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var lang = "en"
    private var counter = 0

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { main.post { onState(true) } }
                    override fun onDone(utteranceId: String?) { main.post { onState(false) } }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { main.post { onState(false) } }
                    override fun onStop(utteranceId: String?, interrupted: Boolean) { main.post { onState(false) } }
                })
            }
        }
    }

    fun setLanguage(l: String) {
        lang = l
    }

    private fun applyLanguage() {
        val t = tts ?: return
        val locale = if (lang == "ne") Locale("ne", "NP") else Locale.US
        val r = t.setLanguage(locale)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            t.setLanguage(Locale.US)
        }
    }

    fun speak(text: String) {
        if (!ready) return
        applyLanguage()
        val safe = if (text.length > 3500) text.substring(0, 3500) else text
        counter++
        tts?.speak(safe, TextToSpeech.QUEUE_FLUSH, null, "jarvis_$counter")
    }

    fun stop() {
        tts?.stop()
        main.post { onState(false) }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
