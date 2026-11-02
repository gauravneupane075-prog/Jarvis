package com.gaurav.jarvis

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Conversation history kept only on this phone, in app-private storage. */
class History(ctx: Context, private val prefs: Prefs) {
    data class Entry(val who: String, val text: String, val time: Long)

    private val sp = ctx.getSharedPreferences("jarvis_history", Context.MODE_PRIVATE)

    private fun load(): JSONArray = try {
        JSONArray(sp.getString("h", "[]"))
    } catch (e: Exception) {
        JSONArray()
    }

    fun add(who: String, text: String) {
        if (!prefs.saveHistory) return
        val arr = load()
        arr.put(JSONObject().put("w", who).put("t", text).put("ts", System.currentTimeMillis()))
        val out = JSONArray()
        for (i in maxOf(0, arr.length() - 100) until arr.length()) out.put(arr.get(i))
        sp.edit().putString("h", out.toString()).apply()
    }

    fun all(): List<Entry> {
        val arr = load()
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Entry(o.optString("w"), o.optString("t"), o.optLong("ts"))
        }
    }

    fun clear() {
        sp.edit().remove("h").apply()
    }
}
