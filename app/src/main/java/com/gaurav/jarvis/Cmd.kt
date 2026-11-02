package com.gaurav.jarvis

/** Every action JARVIS can perform. Both the offline parser and the AI backend produce these. */
sealed class Cmd {
    data class Reply(val text: String) : Cmd()
    data class Ask(val text: String) : Cmd()
    data class Call(val name: String) : Cmd()
    data class Sms(val name: String, val body: String) : Cmd()
    data class WhatsApp(val name: String, val body: String) : Cmd()
    data class OpenApp(val name: String) : Cmd()
    data class WebSearch(val query: String) : Cmd()
    data class Maps(val query: String) : Cmd()
    data class Alarm(val hour: Int, val minute: Int) : Cmd()
    data class Timer(val seconds: Int) : Cmd()
    data class Remind(val title: String, val hour: Int?, val minute: Int?) : Cmd()
    data class Flashlight(val on: Boolean) : Cmd()
    data class Volume(val up: Boolean) : Cmd()
    data class OpenSettings(val which: String) : Cmd()
    data class Photos(val range: String, val video: Boolean) : Cmd()
    data class Pay(val amount: String, val to: String) : Cmd()
    object Battery : Cmd()
    object DeviceInfo : Cmd()
    object ReadNotifications : Cmd()
    object ReadSms : Cmd()
    object Camera : Cmd()
    object StopSpeaking : Cmd()
    object Time : Cmd()
    object Help : Cmd()
}
