package com.gaurav.jarvis

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    var backendUrl: String
        get() = sp.getString("url", "") ?: ""
        set(v) { sp.edit().putString("url", v.trim()).apply() }

    /** Stored encrypted with the Android Keystore. */
    var token: String
        get() {
            val raw = sp.getString("tok", "") ?: ""
            return if (raw.isEmpty()) "" else (SecureStore.decrypt(raw) ?: "")
        }
        set(v) { sp.edit().putString("tok", if (v.isEmpty()) "" else SecureStore.encrypt(v)).apply() }

    /** Your own Claude API key, typed in Settings. Encrypted with the Android Keystore; never in the source code. */
    var apiKey: String
        get() {
            val raw = sp.getString("akey", "") ?: ""
            return if (raw.isEmpty()) "" else (SecureStore.decrypt(raw) ?: "")
        }
        set(v) { sp.edit().putString("akey", if (v.isEmpty()) "" else SecureStore.encrypt(v)).apply() }

    var language: String
        get() = sp.getString("lang", "en") ?: "en"
        set(v) { sp.edit().putString("lang", v).apply() }

    var saveHistory: Boolean
        get() = sp.getBoolean("save_history", true)
        set(v) { sp.edit().putBoolean("save_history", v).apply() }

    var handsFree: Boolean
        get() = sp.getBoolean("hands_free", false)
        set(v) { sp.edit().putBoolean("hands_free", v).apply() }
}
