package com.frogsquare.firebase

import android.app.*
import android.content.Context
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.util.Log

import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

import org.godotengine.godot.Dictionary

private const val TAG: String = "GDFirebase"
private const val NOTIFICATION_REQUEST_ID = 8001

object Notifications {
    @JvmStatic
    fun show(context: Context, params: Dictionary) {
        val title = params["title"] as String?
        val body = params["message_body"] as String?

        if (title == null || (body == null && !params.contains("image_url")))  return

        val channelId = params["channelId"] as String?
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId ?: Utils.DEFAULT_CHANNEL_ID,
                "${channelId ?: "Base"} Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            manager.createNotificationChannel(notificationChannel)
        }

        val iconId = Utils.getMipmapID(context, "icon")
        val notificationIconId = Utils.getMipmapID(context, "notification_icon")
        val icon = BitmapFactory.decodeResource(context.resources, iconId)

        val builder = NotificationCompat.Builder(context, channelId ?: Utils.DEFAULT_CHANNEL_ID)
            .setDefaults(Notification.DEFAULT_ALL)
            .setSmallIcon(if (notificationIconId <= 0) iconId else notificationIconId)
            .setLargeIcon(icon)
            .setContentTitle(title)
            .setContentIntent(Utils.getMainIntent(context))
            .setAutoCancel(true)
            .setShowWhen(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        if (body != null || !params.contains("image_url")) {
            builder.setContentText(body ?: "")
        } else {
            val url = params["image_url"] as String
            if (url.startsWith("res://")) {
                val style = NotificationCompat.BigPictureStyle().bigPicture(
                    Utils.getBitmapFromAsset(context, url)
                )
                builder.setStyle(style)
            } else {
                Log.i(TAG, "Remote Images in Notification is not yet implemented.")
                Log.d(TAG, "Notification image URL should start with `res://` rolling back to Icon.")

                val style = NotificationCompat.BigPictureStyle().bigPicture(icon)
                builder.setStyle(style)
            }
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val soundUrl = params["sound_url"] as String?
        val defaultSound = params["default_sound"] as Boolean?

        if (soundUrl != null) {
            builder.setSound(Uri.parse(soundUrl) ?: defaultSoundUri)
        } else if (defaultSound == null || defaultSound) {
            builder.setSound(defaultSoundUri)
        }

        params["color"]?.let {
            builder.setColorized(true)
            builder.color = it as Int
        }

        manager.notify(NOTIFICATION_REQUEST_ID, builder.build())
    }

    @JvmStatic
    @Suppress("UNUSED", "UNUSED_PARAMETER")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun showNotificationInTime(context: Context, params: Dictionary, delay: Long) {}
}