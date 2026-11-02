package com.gaurav.jarvis

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Contact(val name: String, val number: String)

/**
 * Runs commands using official Android APIs only. Nothing here claims success unless Android
 * confirms it; for things Android won't let apps do silently (send SMS/WhatsApp, pay), JARVIS
 * opens the proper app and tells you that YOU must press send / approve.
 */
class Executor(private val a: MainActivity) {

    fun run(cmd: Cmd, fromAi: Boolean) {
        if (fromAi) {
            // Commands chosen by the AI always need your approval first (protects against bad web content).
            a.confirm("Allow JARVIS to do this?", describe(cmd)) { ok ->
                if (ok) exec(cmd) else a.say("Okay, cancelled.")
            }
        } else {
            exec(cmd)
        }
    }

    private fun describe(c: Cmd): String = when (c) {
        is Cmd.Call -> "Call ${c.name}"
        is Cmd.Sms -> "Prepare a text to ${c.name}: \"${c.body}\""
        is Cmd.WhatsApp -> "Prepare a WhatsApp message to ${c.name}: \"${c.body}\""
        is Cmd.OpenApp -> "Open ${c.name}"
        is Cmd.WebSearch -> "Search the web for \"${c.query}\""
        is Cmd.Maps -> "Open maps for \"${c.query}\""
        is Cmd.Alarm -> "Set an alarm for %02d:%02d".format(c.hour, c.minute)
        is Cmd.Timer -> "Start a ${c.seconds}-second timer"
        is Cmd.Flashlight -> if (c.on) "Turn the flashlight on" else "Turn the flashlight off"
        is Cmd.Volume -> if (c.up) "Turn the volume up" else "Turn the volume down"
        is Cmd.OpenSettings -> "Open ${c.which} settings"
        Cmd.Camera -> "Open the camera"
        Cmd.ReadNotifications -> "Read your notifications aloud"
        Cmd.ReadSms -> "Read your latest text message aloud"
        Cmd.Battery -> "Tell you the battery level"
        else -> "Run this action"
    }

    private fun exec(cmd: Cmd) {
        when (cmd) {
            is Cmd.Reply -> a.say(cmd.text)
            is Cmd.Ask -> a.say("I don't know how to do that offline.")
            Cmd.StopSpeaking -> a.stopSpeaking()
            is Cmd.Call -> call(cmd.name)
            is Cmd.Sms -> withContact(cmd.name) { c ->
                a.say("Opening a text to ${c.name}. Please check it and press send yourself.")
                val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(c.number)))
                    .putExtra("sms_body", cmd.body)
                start(i)
            }
            is Cmd.WhatsApp -> withContact(cmd.name) { c ->
                val digits = whatsappNumber(c.number)
                a.say("Opening WhatsApp for ${c.name}. Please check the message and press send yourself.")
                start(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=" + Uri.encode(cmd.body))))
            }
            is Cmd.OpenApp -> openApp(cmd.name)
            is Cmd.WebSearch -> {
                a.say("Searching Google for ${cmd.query}.")
                start(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(cmd.query))))
            }
            is Cmd.Maps -> {
                a.say("Opening maps for ${cmd.query}.")
                start(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(cmd.query))))
            }
            is Cmd.Alarm -> {
                a.say("Asking the clock app to set an alarm for %02d:%02d.".format(cmd.hour, cmd.minute))
                start(
                    Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, cmd.hour)
                        .putExtra(AlarmClock.EXTRA_MINUTES, cmd.minute)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                )
            }
            is Cmd.Timer -> {
                a.say("Asking the clock app to start the timer.")
                start(
                    Intent(AlarmClock.ACTION_SET_TIMER)
                        .putExtra(AlarmClock.EXTRA_LENGTH, cmd.seconds)
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                )
            }
            is Cmd.Remind -> remind(cmd)
            is Cmd.Flashlight -> flashlight(cmd.on)
            is Cmd.Volume -> volume(cmd.up)
            is Cmd.OpenSettings -> openSettings(cmd.which)
            Cmd.Battery -> battery()
            Cmd.DeviceInfo -> a.say(
                "This is a ${Build.MANUFACTURER} ${Build.MODEL} running Android ${Build.VERSION.RELEASE}."
            )
            Cmd.Camera -> {
                a.say("Opening the camera.")
                start(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            }
            Cmd.ReadNotifications -> readNotifications()
            Cmd.ReadSms -> readSms()
            is Cmd.Photos -> photos(cmd.range, cmd.video)
            is Cmd.Pay -> pay(cmd.amount, cmd.to)
            Cmd.Time -> {
                val now = Date()
                val t = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
                val d = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(now)
                a.say("It is $t on $d.")
            }
            Cmd.Help -> a.say(
                "I can call, text, open WhatsApp, open apps, search Google, set alarms and timers, " +
                    "control the flashlight and volume, read notifications and your latest text, " +
                    "find photos, and open payment apps. For questions and news I need the AI backend."
            )
        }
    }

    // ---------- helpers ----------

    private fun start(i: Intent) {
        try {
            a.startActivity(i)
        } catch (e: Exception) {
            a.say("I couldn't open that on this phone.")
        }
    }

    private fun whatsappNumber(raw: String): String {
        var d = raw.filter { it.isDigit() }
        if (d.startsWith("00")) d = d.substring(2)
        // Nepal assumption: a 10-digit local number gets the 977 country code.
        if (d.length == 10) d = "977$d"
        return d
    }

    private fun withContact(name: String, then: (Contact) -> Unit) {
        a.perms.request(arrayOf(Manifest.permission.READ_CONTACTS)) { ok ->
            if (!ok) {
                a.say("I need contacts permission to find $name.")
                return@request
            }
            val found = findContacts(name)
            when {
                found.isEmpty() -> a.say("I couldn't find $name in your contacts.")
                found.size == 1 -> then(found[0])
                else -> {
                    val exact = found.filter { it.name.equals(name, ignoreCase = true) }
                    if (exact.size == 1) {
                        then(exact[0])
                    } else {
                        a.say("I found ${found.size} contacts that match. Please choose one.")
                        a.choose("Which contact?", found.map { "${it.name}  •  ${it.number}" }) { i -> then(found[i]) }
                    }
                }
            }
        }
    }

    private fun findContacts(q: String): List<Contact> {
        val out = LinkedHashMap<String, Contact>()
        val p = ContactsContract.CommonDataKinds.Phone
        try {
            a.contentResolver.query(
                p.CONTENT_URI,
                arrayOf(p.DISPLAY_NAME, p.NUMBER),
                "${p.DISPLAY_NAME} LIKE ?",
                arrayOf("%$q%"),
                "${p.DISPLAY_NAME} ASC"
            )?.use {
                while (it.moveToNext()) {
                    val n = it.getString(0) ?: continue
                    val num = it.getString(1) ?: continue
                    val key = num.filter { c -> c.isDigit() }.takeLast(9)
                    if (!out.containsKey(key)) out[key] = Contact(n, num)
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return out.values.take(6)
    }

    private fun call(name: String) {
        if (name.matches(Regex("^[+\\d\\s-]{5,}$"))) {
            dial(name.filter { it.isDigit() || it == '+' }, name)
        } else {
            withContact(name) { c -> dial(c.number, c.name) }
        }
    }

    private fun dial(number: String, label: String) {
        a.say("Calling $label.")
        a.perms.request(arrayOf(Manifest.permission.CALL_PHONE)) { ok ->
            // Without permission we open the dialer with the number filled in instead.
            val action = if (ok) Intent.ACTION_CALL else Intent.ACTION_DIAL
            start(Intent(action, Uri.parse("tel:" + Uri.encode(number))))
        }
    }

    private fun launcherApps(): List<Pair<String, String>> {
        val pm = a.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(main, 0).map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
    }

    private fun findApp(name: String): Pair<String, Intent>? {
        val q = name.lowercase().trim()
        if (q.isEmpty()) return null
        val apps = launcherApps()
        val hit = apps.firstOrNull { it.first.lowercase() == q }
            ?: apps.firstOrNull { it.first.lowercase().contains(q) }
            ?: apps.firstOrNull { it.first.length > 3 && q.contains(it.first.lowercase()) }
            ?: return null
        val intent = a.packageManager.getLaunchIntentForPackage(hit.second) ?: return null
        return hit.first to intent
    }

    private fun openApp(name: String) {
        val app = findApp(name)
        if (app == null) {
            a.say("I couldn't find an app called $name on this phone.")
        } else {
            a.say("Opening ${app.first}.")
            start(app.second)
        }
    }

    private fun remind(c: Cmd.Remind) {
        val cal = Calendar.getInstance()
        if (c.hour != null && c.minute != null) {
            cal.set(Calendar.HOUR_OF_DAY, c.hour)
            cal.set(Calendar.MINUTE, c.minute)
            cal.set(Calendar.SECOND, 0)
            if (cal.timeInMillis < System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        } else {
            cal.add(Calendar.HOUR_OF_DAY, 1)
        }
        a.say("Opening your calendar with the reminder filled in. Please tap save to keep it.")
        start(
            Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, c.title)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.timeInMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cal.timeInMillis + 30 * 60 * 1000)
        )
    }

    private fun flashlight(on: Boolean) {
        try {
            val cm = a.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (id == null) {
                a.say("This phone has no flashlight I can use.")
                return
            }
            cm.setTorchMode(id, on)
            a.say(if (on) "Flashlight on." else "Flashlight off.")
        } catch (e: Exception) {
            a.say("I couldn't control the flashlight.")
        }
    }

    private fun volume(up: Boolean) {
        val am = a.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        a.say("Media volume is now $cur of $max.")
    }

    private fun openSettings(which: String) {
        val (action, label) = when (which) {
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS to "Bluetooth"
            "wifi" -> Settings.ACTION_WIFI_SETTINGS to "Wi-Fi"
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS to "location"
            else -> Settings.ACTION_DISPLAY_SETTINGS to "display and brightness"
        }
        a.say("Android doesn't let apps switch this directly, so I opened the $label settings for you.")
        start(Intent(action))
    }

    private fun battery() {
        val i = a.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        if (level < 0 || scale <= 0) {
            a.say("I couldn't read the battery level.")
            return
        }
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        a.say("Battery is at ${level * 100 / scale} percent${if (charging) " and charging" else ""}.")
    }

    /** Hides one-time codes so they are never read aloud or shown on screen by JARVIS. */
    private fun redact(s: String): String {
        val looksLikeCode = Regex("\\b\\d{4,8}\\b").containsMatchIn(s) &&
            Regex("otp|code|verification|password|pin", RegexOption.IGNORE_CASE).containsMatchIn(s)
        return if (looksLikeCode) "[sensitive code hidden]" else s
    }

    private fun readNotifications() {
        val svc = JarvisNotificationService.instance
        if (svc == null) {
            a.say("Notification access is off. I'm opening the setting. Please turn on JARVIS there, then ask again.")
            start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            return
        }
        try {
            val pm = a.packageManager
            val items = svc.activeNotifications
                .filter { it.isClearable && it.packageName != a.packageName }
                .take(5)
            if (items.isEmpty()) {
                a.say("You have no notifications.")
                return
            }
            val lines = items.map { sbn ->
                val app = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
                } catch (e: Exception) {
                    sbn.packageName
                }
                val ex = sbn.notification.extras
                val title = ex.getCharSequence("android.title")?.toString() ?: ""
                val text = ex.getCharSequence("android.text")?.toString() ?: ""
                "$app: " + redact("$title. $text".trim())
            }
            a.say("You have ${items.size} recent notifications. " + lines.joinToString(" Next. "))
        } catch (e: Exception) {
            a.say("I couldn't read your notifications.")
        }
    }

    private fun readSms() {
        a.perms.request(arrayOf(Manifest.permission.READ_SMS)) { ok ->
            if (!ok) {
                a.say("I need permission to read text messages.")
                return@request
            }
            try {
                a.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
                    null, null, "date DESC"
                )?.use {
                    if (it.moveToFirst()) {
                        val from = it.getString(0) ?: "unknown sender"
                        val body = redact(it.getString(1) ?: "")
                        a.say("Latest text from $from. $body")
                    } else {
                        a.say("You have no text messages.")
                    }
                } ?: a.say("I couldn't read your messages.")
            } catch (e: Exception) {
                a.say("I couldn't read your messages.")
            }
        }
    }

    private fun photos(range: String, video: Boolean) {
        a.perms.request(Permissions.mediaPerms()) { ok ->
            if (!ok) {
                a.say("I need photo access to search your gallery.")
                return@request
            }
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val todayStart = cal.timeInMillis / 1000
            val nowSec = System.currentTimeMillis() / 1000
            val (lo, hi) = when (range) {
                "today" -> todayStart to Long.MAX_VALUE
                "yesterday" -> (todayStart - 86400) to todayStart
                "week" -> (nowSec - 7 * 86400) to Long.MAX_VALUE
                else -> 0L to Long.MAX_VALUE
            }
            val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val kind = if (video) "video" else "photo"
            try {
                a.contentResolver.query(
                    base,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DATE_ADDED} >= ? AND ${MediaStore.MediaColumns.DATE_ADDED} < ?",
                    arrayOf(lo.toString(), hi.toString()),
                    "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                )?.use {
                    if (it.moveToFirst()) {
                        val id = it.getLong(0)
                        val n = it.count
                        a.say("I found $n ${kind}s. Opening the newest one.")
                        start(
                            Intent(Intent.ACTION_VIEW)
                                .setDataAndType(ContentUris.withAppendedId(base, id), if (video) "video/*" else "image/*")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        )
                    } else {
                        a.say("I didn't find any ${kind}s for that time.")
                    }
                } ?: a.say("I couldn't search your gallery.")
            } catch (e: Exception) {
                a.say("I couldn't search your gallery.")
            }
        }
    }

    private fun pay(amount: String, to: String) {
        a.say("Please check the payment details on the screen. I can't make payments myself.")
        a.confirm(
            "Open payment app?",
            "Recipient: $to\nAmount: NPR $amount\n\nJARVIS never pays for you. It will only open your payment app, " +
                "where you must enter the details and approve with your own PIN or fingerprint."
        ) { ok ->
            if (!ok) {
                a.say("Payment cancelled.")
                return@confirm
            }
            val keywords = listOf("esewa", "khalti", "fonepay", "imepay", "ime pay", "connectips", "mobile banking", "banking")
            val apps = launcherApps()
            val hit = keywords.firstNotNullOfOrNull { k -> apps.firstOrNull { it.first.lowercase().contains(k) } }
            val intent = hit?.let { a.packageManager.getLaunchIntentForPackage(it.second) }
            if (hit == null || intent == null) {
                a.say("I couldn't find a payment app on this phone. Please open your banking or wallet app yourself.")
            } else {
                a.say("Opening ${hit.first}. Enter the details and approve there. I can't confirm if the payment succeeds.")
                start(intent)
            }
        }
    }
}
