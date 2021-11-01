package com.frogsquare.firebasecm

import android.util.Log
import com.frogsquare.firebase.GDFirebase
import com.google.firebase.messaging.FirebaseMessaging
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot
import com.google.firebase.messaging.RemoteMessage
import org.godotengine.godot.Dictionary
import java.nio.charset.Charset
import java.security.MessageDigest

private const val TAG = "GDFirebaseCM"

@Suppress("UNUSED")
class GDFirebaseCloudMessaging constructor(godot: Godot): GodotPlugin(godot) {

    private val instance: FirebaseMessaging = FirebaseMessaging.getInstance()
    private var token: String? = null

    init {
        instance.token.addOnCompleteListener { task ->
            val devMode = GDFirebase.isDevelopment
            if (task.isSuccessful) {
                val result = task.result
                token = result

                if (devMode)
                    Log.i(TAG, "Got CM token $token")
            }
        }
    }

    @UsedByGodot
    fun subscribe(topic: String) {
        instance.subscribeToTopic(topic)
    }

    @UsedByGodot
    fun unsubscribe(topic: String) {
        instance.unsubscribeFromTopic(topic)
    }

    override fun getPluginName(): String {
        return "GDFirebaseCloudMessaging"
    }
}
