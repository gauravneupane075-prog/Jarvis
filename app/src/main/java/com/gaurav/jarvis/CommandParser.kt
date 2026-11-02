package com.gaurav.jarvis

/** Offline command understanding (works without internet). Anything unknown becomes Cmd.Ask. */
object CommandParser {
    private val wake = Regex("^\\s*(?:hey|hi|ok|okay)?\\s*jarvis\\b[,.!:]?\\s*", RegexOption.IGNORE_CASE)

    /** Returns the text after "Hey Jarvis", or null if the wake phrase is not at the start. */
    fun stripWake(s: String): String? {
        val m = wake.find(s) ?: return null
        return s.substring(m.range.last + 1).trim()
    }

    fun parseTime(s: String): Pair<Int, Int>? {
        Regex("(\\d{1,2})(?:[:.](\\d{2}))?\\s*([ap])\\.?m\\b", RegexOption.IGNORE_CASE).find(s)?.let {
            var h = it.groupValues[1].toInt()
            val min = it.groupValues[2].ifEmpty { "0" }.toInt()
            val pm = it.groupValues[3].equals("p", true)
            if (h in 1..12 && min in 0..59) {
                if (h == 12) h = 0
                if (pm) h += 12
                return h to min
            }
        }
        Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b").find(s)?.let {
            return it.groupValues[1].toInt() to it.groupValues[2].toInt()
        }
        return null
    }

    fun parse(raw: String): Cmd {
        val s = (stripWake(raw) ?: raw).trim().trimEnd('.', '!', '?', '।').trim()
        if (s.isEmpty()) return Cmd.Reply("Yes?")
        val low = s.lowercase()
        fun m(r: String) = Regex(r, RegexOption.IGNORE_CASE).find(s)

        if (low in setOf("stop", "stop talking", "be quiet", "quiet", "cancel", "stop speaking", "चुप", "रोक")) {
            return Cmd.StopSpeaking
        }
        if (low == "time" || low == "date" || low.contains("what time") || low.contains("what's the time") ||
            low.contains("what day") || low.contains("today's date") || low.contains("what is the date")
        ) return Cmd.Time
        if (low == "help" || low.contains("what can you do")) return Cmd.Help

        // WhatsApp
        m("^(?:send|message|text)\\s+(.+?)\\s+(?:a\\s+)?whatsapp(?:\\s+message)?\\s*(?:saying|that says|that|:)?\\s*(.*)$")?.let {
            val n = it.groupValues[1]; val b = it.groupValues[2]
            return if (b.isBlank()) Cmd.Reply("What should the WhatsApp message to $n say?") else Cmd.WhatsApp(n, b)
        }
        m("^whatsapp\\s+(.+?)\\s+(?:saying|that|:)\\s*(.+)$")?.let {
            return Cmd.WhatsApp(it.groupValues[1], it.groupValues[2])
        }

        // Read SMS / notifications (before SMS compose)
        if (m("^(?:read|check|show)\\s+(?:my\\s+)?(?:(?:latest|last|new|recent)\\s+)?(?:sms|text messages?|messages?)$") != null) {
            return Cmd.ReadSms
        }
        if (low.contains("notification") && Regex("read|check|show|what|any|list").containsMatchIn(low)) {
            return Cmd.ReadNotifications
        }

        // SMS compose
        m("^(?:send|text)\\s+(.+?)\\s+(?:an?\\s+)?(?:sms|text(?: message)?|message)\\s*(?:saying|that says|that|:)?\\s*(.*)$")?.let {
            val n = it.groupValues[1]; val b = it.groupValues[2]
            return if (b.isBlank()) Cmd.Reply("What should the message to $n say?") else Cmd.Sms(n, b)
        }

        // Calls
        m("^(?:call|phone|dial|ring)\\s+(.+)$")?.let { return Cmd.Call(it.groupValues[1].trim()) }
        m("^(.+?)\\s*लाई\\s*(?:फोन|कल)(?:\\s*गर\\S*)?$")?.let { return Cmd.Call(it.groupValues[1].trim()) }

        // Device controls
        if (Regex("flash ?light|torch|टर्च").containsMatchIn(low)) {
            return Cmd.Flashlight(!Regex("\\boff\\b|बन्द").containsMatchIn(low))
        }
        if (low.contains("volume")) {
            return when {
                Regex("increase|up|raise|louder|higher|बढ").containsMatchIn(low) -> Cmd.Volume(true)
                Regex("decrease|down|lower|reduce|quieter|घट").containsMatchIn(low) -> Cmd.Volume(false)
                else -> Cmd.Reply("Do you want the volume up or down?")
            }
        }
        if (low.contains("bluetooth")) return Cmd.OpenSettings("bluetooth")
        if (Regex("wi-?fi").containsMatchIn(low)) return Cmd.OpenSettings("wifi")
        if (low.contains("location settings") || low.contains("turn on location") || low.contains("turn off location")) {
            return Cmd.OpenSettings("location")
        }
        if (low.contains("brightness") || low.contains("display setting")) return Cmd.OpenSettings("display")
        if (low.contains("battery")) return Cmd.Battery
        if (low.contains("device info") || low.contains("phone info") || low.contains("about my phone")) return Cmd.DeviceInfo
        if (Regex("^(?:open|launch|start)\\s+(?:my\\s+|the\\s+)?camera$|take a (?:photo|picture|selfie)").containsMatchIn(low)) {
            return Cmd.Camera
        }

        // Alarms, timers, reminders
        if (low.contains("alarm")) {
            if (Regex("cancel|delete|remove|turn off").containsMatchIn(low)) {
                return Cmd.Reply("I can't remove alarms yet. Please open the Clock app to cancel it.")
            }
            val t = parseTime(low) ?: return Cmd.Reply("Please tell me the alarm time with AM or PM, for example 6 AM.")
            return Cmd.Alarm(t.first, t.second)
        }
        if (low.contains("timer")) {
            Regex("(\\d+)\\s*(second|sec|minute|min|hour|hr)").find(low)?.let {
                val n = it.groupValues[1].toInt()
                val secs = when (it.groupValues[2]) {
                    "second", "sec" -> n
                    "minute", "min" -> n * 60
                    else -> n * 3600
                }
                return Cmd.Timer(secs)
            }
            return Cmd.Reply("For how long? For example, say: set a timer for 5 minutes.")
        }
        m("^remind me (?:to )?(.+?)(?:\\s+at\\s+(.+))?$")?.let {
            val t = if (it.groupValues[2].isNotBlank()) parseTime(it.groupValues[2]) else null
            return Cmd.Remind(it.groupValues[1].trim(), t?.first, t?.second)
        }

        // Photos / videos
        if (Regex("photo|picture|\\bpics?\\b|image|video").containsMatchIn(low) &&
            Regex("find|show|open|latest|last|recent|from|get").containsMatchIn(low)
        ) {
            val range = when {
                low.contains("yesterday") -> "yesterday"
                low.contains("today") -> "today"
                low.contains("week") -> "week"
                else -> "latest"
            }
            return Cmd.Photos(range, low.contains("video"))
        }

        // Payments (never automatic: always confirmed, then opens your payment app)
        m("^(?:pay|send)\\s+(?:npr|rs\\.?|rupees?)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:npr|rs\\.?|rupees?)?\\s+(?:to\\s+)?(.+)$")?.let {
            return Cmd.Pay(it.groupValues[1], it.groupValues[2].trim())
        }

        // Maps
        m("^(?:navigate to|directions to|take me to|how do i get to|nearby|find nearby|show me nearby)\\s+(.+)$")?.let {
            val g = it.groupValues[1].trim()
            return Cmd.Maps(if (low.contains("nearby")) "$g near me" else g)
        }

        m("^play\\s+(.+)$")?.let { return Cmd.PlayMusic(it.groupValues[1].trim()) }

        // Web search (opens the browser)
        m("^(?:search|google|look up|lookup)\\s+(?:on\\s+)?(?:google\\s+)?(?:for\\s+)?(.+)$")?.let {
            return Cmd.WebSearch(it.groupValues[1].trim())
        }

        // Open apps
        m("^(?:open|launch|start)\\s+(?:my\\s+|the\\s+)?(.+?)(?:\\s+app)?$")?.let {
            return Cmd.OpenApp(it.groupValues[1].trim())
        }
        m("^(.+?)\\s*खोल\\S*$")?.let { return Cmd.OpenApp(it.groupValues[1].trim()) }

        return Cmd.Ask(s)
    }
}
