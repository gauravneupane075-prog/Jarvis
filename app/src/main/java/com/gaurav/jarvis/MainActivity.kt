package com.gaurav.jarvis

import android.Manifest
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), Voice.Callback {
    private lateinit var prefs: Prefs
    private lateinit var history: History
    private lateinit var speaker: Speaker
    private lateinit var voice: Voice
    private lateinit var executor: Executor
    private val ai = AiService()
    val perms = Permissions(this)

    private lateinit var vis: VisualizerView
    private lateinit var status: TextView
    private lateinit var net: TextView
    private lateinit var permText: TextView
    private lateinit var convo: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var handsFree: SwitchCompat

    private val handler = Handler(Looper.getMainLooper())
    private val turns = ArrayList<Pair<String, String>>()
    private var aiJob: Job? = null

    private var listening = false
    private var speaking = false
    private var thinking = false
    private var manual = false
    private var awaitCommand = false
    private var inForeground = false
    var dialogOpen = false

    private val ticker = object : Runnable {
        override fun run() {
            refreshStatusBar()
            handler.postDelayed(this, 5000)
        }
    }
    private val resumeRunnable = Runnable {
        if (prefs.handsFree && inForeground && !listening && !speaking && !thinking && !dialogOpen) {
            beginListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        history = History(this, prefs)
        vis = findViewById(R.id.vis)
        status = findViewById(R.id.statusText)
        net = findViewById(R.id.netText)
        permText = findViewById(R.id.permText)
        convo = findViewById(R.id.convo)
        scroll = findViewById(R.id.scroll)
        input = findViewById(R.id.input)
        handsFree = findViewById(R.id.handsFree)

        speaker = Speaker(this) { s ->
            speaking = s
            updateState()
            if (!s) resumeHandsFree()
        }
        voice = Voice(this, this)
        executor = Executor(this)

        findViewById<View>(R.id.mic).setOnClickListener { onMicClicked() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { emergencyStop() }
        findViewById<View>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<View>(R.id.btnHistory).setOnClickListener { showHistory() }
        permText.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<Button>(R.id.btnSend).setOnClickListener { sendTyped() }
        input.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendTyped()
                true
            } else {
                false
            }
        }
        handsFree.isChecked = prefs.handsFree
        handsFree.setOnCheckedChangeListener { _, on -> onHandsFreeToggled(on) }

        addBubble("JARVIS", "Ready. Tap the microphone and speak, or type a command. Say “help” to hear what I can do.")
        updateState()
    }

    override fun onResume() {
        super.onResume()
        inForeground = true
        speaker.setLanguage(prefs.language)
        handsFree.isChecked = prefs.handsFree
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        resumeHandsFree()
    }

    override fun onPause() {
        super.onPause()
        // JARVIS never listens in the background.
        inForeground = false
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(resumeRunnable)
        voice.stop()
        listening = false
        manual = false
        updateState()
    }

    override fun onDestroy() {
        super.onDestroy()
        voice.stop()
        speaker.shutdown()
    }

    // ---------- Voice.Callback ----------

    override fun onListening() {
        updateState()
    }

    override fun onRms(level: Float) {
        vis.level = level
    }

    override fun onResult(text: String) {
        listening = false
        val wasManual = manual
        manual = false
        if (prefs.handsFree && !wasManual && !awaitCommand) {
            val rest = CommandParser.stripWake(text)
            when {
                rest == null -> { updateState(); resumeHandsFree() }
                rest.isBlank() -> { awaitCommand = true; say("Yes?") }
                else -> handle(rest)
            }
        } else {
            awaitCommand = false
            handle(text)
        }
        updateState()
    }

    override fun onFailure(message: String, silent: Boolean, fatal: Boolean) {
        listening = false
        val wasManual = manual
        manual = false
        awaitCommand = false
        updateState()
        if (fatal) {
            if (prefs.handsFree) { prefs.handsFree = false; handsFree.isChecked = false }
            say(message)
            return
        }
        if (message.isNotBlank() && (!silent || wasManual)) say(message)
        if (prefs.handsFree) handler.postDelayed({ resumeHandsFree() }, 800)
    }

    // ---------- Mic and typing ----------

    private fun onMicClicked() {
        if (listening) {
            voice.stop()
            listening = false
            manual = false
            updateState()
        } else {
            manual = true
            awaitCommand = false
            beginListening()
        }
    }

    private fun beginListening() {
        if (listening) return
        if (!voice.available()) {
            manual = false
            say("Speech recognition isn't available on this phone. Please install or enable Google speech services.")
            return
        }
        perms.request(arrayOf(Manifest.permission.RECORD_AUDIO)) { ok ->
            if (!ok) {
                manual = false
                say("I need the microphone permission to hear you. Please allow it.")
                return@request
            }
            speaker.stop()
            listening = true
            voice.start(prefs.language)
            updateState()
        }
    }

    private fun sendTyped() {
        val t = input.text.toString().trim()
        if (t.isEmpty()) return
        input.setText("")
        handle(t)
    }

    private fun onHandsFreeToggled(on: Boolean) {
        prefs.handsFree = on
        if (on) {
            perms.request(arrayOf(Manifest.permission.RECORD_AUDIO)) { ok ->
                if (!ok) {
                    prefs.handsFree = false
                    handsFree.isChecked = false
                    say("Hands-free needs the microphone permission.")
                } else {
                    say("Hands-free is on. This is experimental and works only while this screen is open. Say Hey Jarvis, then your command.")
                }
            }
        } else {
            voice.stop()
            listening = false
            awaitCommand = false
            handler.removeCallbacks(resumeRunnable)
            updateState()
        }
    }

    private fun resumeHandsFree() {
        handler.removeCallbacks(resumeRunnable)
        if (prefs.handsFree) handler.postDelayed(resumeRunnable, 600)
    }

    // ---------- Commands ----------

    private fun handle(text: String) {
        addBubble("You", text)
        history.add("you", text)
        turns.add("user" to text)
        trimTurns()
        when (val c = CommandParser.parse(text)) {
            is Cmd.Ask -> askAi()
            else -> executor.run(c, false)
        }
    }

    private fun askAi() {
        if (prefs.backendUrl.isBlank()) {
            say("I can run phone commands offline. To answer questions, set up the AI backend in Settings.")
            return
        }
        if (!isOnline()) {
            say("You're offline, so I can only run basic phone commands.")
            return
        }
        thinking = true
        updateState()
        aiJob = lifecycleScope.launch {
            val r = ai.ask(prefs.backendUrl, prefs.token, prefs.language, turns.toList())
            thinking = false
            updateState()
            when (r) {
                is AiService.Result.Ok -> {
                    say(r.speech)
                    r.cmd?.let { executor.run(it, true) }
                }
                is AiService.Result.Err -> say(r.message)
            }
        }
    }

    private fun trimTurns() {
        while (turns.size > 12) turns.removeAt(0)
    }

    /** Shows and speaks a reply. */
    fun say(text: String) {
        addBubble("JARVIS", text)
        history.add("jarvis", text)
        turns.add("assistant" to text)
        trimTurns()
        speaker.speak(text)
    }

    fun stopSpeaking() {
        speaker.stop()
    }

    private fun emergencyStop() {
        aiJob?.cancel()
        thinking = false
        speaker.stop()
        voice.stop()
        listening = false
        manual = false
        awaitCommand = false
        handler.removeCallbacks(resumeRunnable)
        prefs.handsFree = false
        handsFree.isChecked = false
        updateState()
        addBubble("JARVIS", "Stopped. Hands-free is off.")
    }

    // ---------- Dialogs ----------

    fun confirm(title: String, msg: String, cb: (Boolean) -> Unit) {
        dialogOpen = true
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Yes") { _, _ -> dialogOpen = false; cb(true) }
            .setNegativeButton("No") { _, _ -> dialogOpen = false; cb(false) }
            .setOnCancelListener { dialogOpen = false; cb(false) }
            .show()
    }

    fun choose(title: String, items: List<String>, cb: (Int) -> Unit) {
        dialogOpen = true
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(items.toTypedArray()) { _, i -> dialogOpen = false; cb(i) }
            .setOnCancelListener { dialogOpen = false; say("Cancelled.") }
            .show()
    }

    private fun showHistory() {
        val fmt = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
        val all = history.all().takeLast(50)
        val msg = if (all.isEmpty()) "No saved history." else
            all.joinToString("\n\n") { "${fmt.format(Date(it.time))}  ${it.who.uppercase()}\n${it.text}" }
        AlertDialog.Builder(this).setTitle("Conversation history").setMessage(msg)
            .setPositiveButton("Close", null).show()
    }

    // ---------- UI ----------

    private fun addBubble(who: String, text: String) {
        val tv = TextView(this)
        tv.text = if (who == "You") "▸ You: $text" else "◆ JARVIS: $text"
        tv.setTextColor(getColor(if (who == "You") R.color.jarvis_text else R.color.jarvis_cyan))
        tv.textSize = 15f
        tv.setPadding(0, 8, 0, 8)
        convo.addView(tv)
        if (convo.childCount > 60) convo.removeViewAt(0)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun updateState() {
        val mode = when {
            speaking -> VisualizerView.Mode.SPEAKING
            listening -> VisualizerView.Mode.LISTENING
            thinking -> VisualizerView.Mode.THINKING
            else -> VisualizerView.Mode.IDLE
        }
        vis.mode = mode
        status.text = when (mode) {
            VisualizerView.Mode.SPEAKING -> "Speaking…"
            VisualizerView.Mode.LISTENING ->
                if (awaitCommand) "Listening for your command…"
                else if (prefs.handsFree && !manual) "Waiting for “Hey Jarvis”…"
                else "Listening…"
            VisualizerView.Mode.THINKING -> "Thinking…"
            VisualizerView.Mode.IDLE -> if (prefs.handsFree) "Hands-free on" else "Tap the mic and speak"
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        val n = cm.activeNetwork ?: return false
        val c = cm.getNetworkCapabilities(n) ?: return false
        return c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun refreshStatusBar() {
        val online = isOnline()
        net.text = if (online) "● Online" else "● Offline"
        net.setTextColor(getColor(if (online) R.color.jarvis_cyan else R.color.jarvis_red))
        fun mark(ok: Boolean) = if (ok) "✓" else "✗"
        permText.text = "Mic ${mark(perms.has(Manifest.permission.RECORD_AUDIO))}  " +
            "Contacts ${mark(perms.has(Manifest.permission.READ_CONTACTS))}  " +
            "Phone ${mark(perms.has(Manifest.permission.CALL_PHONE))}  " +
            "SMS ${mark(perms.has(Manifest.permission.READ_SMS))}  " +
            "Notifications ${mark(JarvisNotificationService.instance != null)}  (tap to manage)"
    }
}
