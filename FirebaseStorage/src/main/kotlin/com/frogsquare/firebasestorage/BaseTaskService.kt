package com.frogsquare.firebasestorage

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.Log

import androidx.core.app.NotificationCompat
import com.frogsquare.firebase.Utils

const val PROGRESS_NOTIFICATION_ID = 0x0006
const val COMPLETE_NOTIFICATION_ID = 0x0007

@Suppress("UNUSED")
abstract class BaseTaskService: Service() {
    private var mNumTasks: Int = 0
    private val manager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun taskStarted() {
        changeNumberOfTasks(1)
    }

    fun taskCompleted() {
        changeNumberOfTasks(-1)
    }

    @Synchronized
    private fun changeNumberOfTasks(delta: Int) {
        Log.d(TAG, "ChangedNumberOfTask::$mNumTasks:By::$delta")
        mNumTasks += delta

        if (mNumTasks <= 0) {
            Log.d(TAG, "Completed")
            stopSelf()
        }
    }

    protected fun showProgressNotification(caption: String, completeUnits: Long, totalUnits: Long) {
        var percentComplete = 0
        if (totalUnits > 0) {
            percentComplete = (100 * completeUnits / totalUnits).toInt()
        }

        val labelId = Utils.getResourceID(this, "godot_project_name_string", "string")

        Utils.createDefaultChannel(manager)
        val builder = NotificationCompat.Builder(this, Utils.DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_file_upload_white_24dp)
            .setContentTitle(getString(labelId))
            .setContentText(caption)
            .setProgress(100, percentComplete, false)
            .setOngoing(true)
            .setAutoCancel(false)

        manager.notify(PROGRESS_NOTIFICATION_ID, builder.build())
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    protected fun showCompletedNotification(caption: String, intent: Intent, success: Boolean) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val icon = if (success) R.drawable.ic_check_white_24  else R.drawable.ic_error_white_24dp
        val labelId = Utils.getResourceID(this, "godot_project_name_string", "string")

        Utils.createDefaultChannel(manager)
        val builder = NotificationCompat.Builder(this, Utils.DEFAULT_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(getString(labelId))
            .setContentText(caption)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        manager.notify(COMPLETE_NOTIFICATION_ID, builder.build())
    }

    protected fun dismissProgressNotification() {
        manager.cancel(PROGRESS_NOTIFICATION_ID)
    }
}