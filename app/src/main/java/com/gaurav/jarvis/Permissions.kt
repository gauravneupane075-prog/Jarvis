package com.gaurav.jarvis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/** Asks for a runtime permission only when a command needs it. Create it as an Activity field. */
class Permissions(private val activity: ComponentActivity) {
    private var pending: ((Boolean) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val cb = pending
        pending = null
        cb?.invoke(result.values.all { it })
    }

    fun has(p: String) = ContextCompat.checkSelfPermission(activity, p) == PackageManager.PERMISSION_GRANTED

    fun request(perms: Array<String>, cb: (Boolean) -> Unit) {
        if (perms.all { has(it) }) {
            cb(true)
            return
        }
        pending = cb
        launcher.launch(perms)
    }

    companion object {
        fun mediaPerms(): Array<String> =
            if (Build.VERSION.SDK_INT >= 33) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        fun allRuntime(): Array<String> = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_SMS
        ) + mediaPerms()
    }
}
