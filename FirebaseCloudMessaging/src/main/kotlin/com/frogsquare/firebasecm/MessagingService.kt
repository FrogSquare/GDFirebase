package com.frogsquare.firebasecm

import android.content.Context
import android.util.Log
import com.frogsquare.firebase.Common

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import com.frogsquare.firebase.Notifications
import org.godotengine.godot.Dictionary
import org.json.JSONException
import org.json.JSONObject

private const val TAG: String = "GDFirebaseCM"

class MessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Message, From: ${message.data} ${message.from}")

        if (message.data.isNotEmpty()) {
            val prefs = getSharedPreferences(Common.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
            val shouldSave = prefs.getBoolean("should_save", true)

            if (shouldSave) {
                val json = JSONObject()
                try {
                    for (pair in message.data) {
                        json.put(pair.key, pair.value)
                    }
                } catch (e: JSONException) {
                    Log.e(TAG, "JSON Parsing failed ${e.message}\n" + Log.getStackTraceString(e))
                    return
                }

                prefs.edit().putString(message.from, json.toString()).apply()
            }
        }

        message.notification?.let {
            showNotification(it)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token $token")
        sendRegistrationToServer(token)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement this method to send token to your app server.
    }

    private fun showNotification(info: RemoteMessage.Notification?) {
        if (info == null) return

        val params = Dictionary()
        params["title"] = info.title ?: "FCM"
        params["message_body"] = info.body
        params["channelId"] = info.channelId
        params["color"] = info.color
        params["ticker"] = info.ticker
        params["sound_url"] = info.sound
        params["default_sound"] = info.defaultSound
        params["image_url"] = info.imageUrl

        Notifications.show(this, params)
    }
}