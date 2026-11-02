package com.gaurav.jarvis

import android.service.notification.NotificationListenerService

/**
 * Lets JARVIS read your current notifications, only after you turn on "Notification access"
 * for JARVIS in Android settings. Nothing is stored or sent anywhere by this service.
 */
class JarvisNotificationService : NotificationListenerService() {
    companion object {
        @Volatile
        var instance: JarvisNotificationService? = null
    }

    override fun onListenerConnected() {
        instance = this
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
    }
}
