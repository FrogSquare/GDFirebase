package com.frogsquare.firebase


import android.content.Context
import android.util.Log

import androidx.work.Worker
import androidx.work.WorkerParameters
import org.godotengine.godot.Dictionary

@Suppress("UNUSED")
class NotifyInTime(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("GDFirebase", "Performing long running task in scheduled job")

        val labelId = Utils.getResourceID(applicationContext, "godot_project_name_string", "string")
        val label = applicationContext.getString(labelId)

        val params = Dictionary()
        params["title"] = inputData.getString("title") ?: label
        params["message_body"] = inputData.getString("message") ?: ""
        params["channelId"] = inputData.getString("channelId")

        Notifications.show(applicationContext, params)

        return Result.success()
    }
}