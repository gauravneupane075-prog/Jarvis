package com.gaurav.jarvis

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private val perms = Permissions(this)
    private lateinit var etUrl: EditText
    private lateinit var etToken: EditText
    private lateinit var tvPerms: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)
        etUrl = findViewById(R.id.etUrl)
        etToken = findViewById(R.id.etToken)
        tvPerms = findViewById(R.id.tvPerms)

        etUrl.setText(prefs.backendUrl)
        if (prefs.token.isNotEmpty()) etToken.hint = "Token saved (type to replace)"

        val rbEn = findViewById<RadioButton>(R.id.rbEn)
        val rbNe = findViewById<RadioButton>(R.id.rbNe)
        if (prefs.language == "ne") rbNe.isChecked = true else rbEn.isChecked = true
        rbEn.setOnClickListener { prefs.language = "en" }
        rbNe.setOnClickListener { prefs.language = "ne" }

        val sw = findViewById<SwitchCompat>(R.id.swHistory)
        sw.isChecked = prefs.saveHistory
        sw.setOnCheckedChangeListener { _, on -> prefs.saveHistory = on }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            History(this, prefs).clear()
            Toast.makeText(this, "History deleted", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnGrant).setOnClickListener {
            perms.request(Permissions.allRuntime()) { refreshPerms() }
        }
        findViewById<Button>(R.id.btnNotif).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.btnTest).setOnClickListener { saveAndTest() }

        findViewById<TextView>(R.id.tvPrivacy).text =
            "• Your contacts, messages, photos and notifications stay on this phone.\n" +
            "• Only what you say or type for AI questions (plus the last few messages) is sent to your backend, " +
            "which forwards it to Claude. Phone commands such as calling or opening apps work offline.\n" +
            "• JARVIS only listens while this app is open and you have tapped the mic or turned on hands-free.\n" +
            "• JARVIS never sends texts, WhatsApp messages or payments by itself. You always press send or approve.\n" +
            "• One-time codes in texts and notifications are never read aloud.\n" +
            "• History is stored privately on this phone. You can turn it off or delete it here."
        findViewById<TextView>(R.id.tvAbout).text =
            "JARVIS 1.0.0\nA voice assistant for Android. Claude AI answers questions through your own backend. " +
            "The API key is never inside this app."
        refreshPerms()
    }

    override fun onResume() {
        super.onResume()
        refreshPerms()
    }

    private fun refreshPerms() {
        fun m(ok: Boolean) = if (ok) "✓ allowed" else "✗ not allowed"
        tvPerms.text = "Microphone: ${m(perms.has(android.Manifest.permission.RECORD_AUDIO))}\n" +
            "Contacts: ${m(perms.has(android.Manifest.permission.READ_CONTACTS))}\n" +
            "Phone calls: ${m(perms.has(android.Manifest.permission.CALL_PHONE))}\n" +
            "Read texts: ${m(perms.has(android.Manifest.permission.READ_SMS))}\n" +
            "Notification access: ${m(JarvisNotificationService.instance != null)}"
    }

    private fun saveAndTest() {
        prefs.backendUrl = etUrl.text.toString()
        val t = etToken.text.toString().trim()
        if (t.isNotEmpty()) {
            prefs.token = t
            etToken.setText("")
            etToken.hint = "Token saved (type to replace)"
        }
        if (prefs.backendUrl.isBlank()) {
            Toast.makeText(this, "Enter the backend URL first", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "Testing…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val r = AiService().ask(prefs.backendUrl, prefs.token, prefs.language, listOf("user" to "Say hello in five words."))
            val msg = when (r) {
                is AiService.Result.Ok -> "Connected ✓  " + r.speech
                is AiService.Result.Err -> r.message
            }
            Toast.makeText(this@SettingsActivity, msg, Toast.LENGTH_LONG).show()
        }
    }
}
