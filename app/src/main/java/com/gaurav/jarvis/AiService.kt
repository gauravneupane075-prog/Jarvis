package com.gaurav.jarvis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Talks to YOUR backend (which holds the Claude API key). The app never contains the key. */
class AiService {
    sealed class Result {
        data class Ok(val speech: String, val cmd: Cmd?) : Result()
        data class Err(val message: String) : Result()
    }

    suspend fun ask(url: String, token: String, lang: String, turns: List<Pair<String, String>>): Result =
        withContext(Dispatchers.IO) {
            if (!url.startsWith("https://")) {
                return@withContext Result.Err("The backend address must start with https://. Please fix it in Settings.")
            }
            var conn: HttpURLConnection? = null
            try {
                val msgs = JSONArray()
                turns.dropWhile { it.first != "user" }.forEach {
                    msgs.put(JSONObject().put("role", it.first).put("content", it.second))
                }
                val body = JSONObject().put("messages", msgs).put("language", lang).toString()
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 60000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                }
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) {
                    return@withContext Result.Err(
                        when (code) {
                            401, 403 -> "The backend rejected my access token. Please check the token in Settings."
                            else -> "The AI backend returned an error ($code)."
                        }
                    )
                }
                val j = JSONObject(text)
                val cmd = toCmd(j.optJSONObject("action"))
                val speech = j.optString("speech", "").ifBlank {
                    if (cmd != null) "Okay." else "I don't have an answer for that."
                }
                Result.Ok(speech, cmd)
            } catch (e: java.net.UnknownHostException) {
                Result.Err("I can't reach the AI backend. Please check your internet connection and the backend address.")
            } catch (e: java.net.SocketTimeoutException) {
                Result.Err("The AI backend took too long to answer. Please try again.")
            } catch (e: Exception) {
                Result.Err("The AI backend gave an answer I couldn't understand.")
            } finally {
                conn?.disconnect()
            }
        }

    private fun toCmd(a: JSONObject?): Cmd? {
        if (a == null) return null
        fun s(k: String) = a.optString(k).trim()
        return when (s("type")) {
            "call" -> s("name").takeIf { it.isNotBlank() }?.let { Cmd.Call(it) }
            "sms" -> if (s("name").isNotBlank() && s("body").isNotBlank()) Cmd.Sms(s("name"), s("body")) else null
            "whatsapp" -> if (s("name").isNotBlank() && s("body").isNotBlank()) Cmd.WhatsApp(s("name"), s("body")) else null
            "open_app" -> s("name").takeIf { it.isNotBlank() }?.let { Cmd.OpenApp(it) }
            "web_search" -> s("query").takeIf { it.isNotBlank() }?.let { Cmd.WebSearch(it) }
            "maps" -> s("query").takeIf { it.isNotBlank() }?.let { Cmd.Maps(it) }
            "alarm" -> {
                val h = a.optInt("hour", -1)
                val m = a.optInt("minute", 0)
                if (h in 0..23 && m in 0..59) Cmd.Alarm(h, m) else null
            }
            "timer" -> a.optInt("seconds", 0).takeIf { it in 1..86400 }?.let { Cmd.Timer(it) }
            "flashlight" -> Cmd.Flashlight(a.optBoolean("on", true))
            "volume" -> Cmd.Volume(s("direction") == "up")
            "settings" -> s("which").takeIf { it in setOf("bluetooth", "wifi", "location", "display") }
                ?.let { Cmd.OpenSettings(it) }
            "camera" -> Cmd.Camera
            "read_notifications" -> Cmd.ReadNotifications
            "read_sms" -> Cmd.ReadSms
            "battery" -> Cmd.Battery
            else -> null
        }
    }
}
