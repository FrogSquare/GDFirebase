package com.frogsquare.firebasemessaging

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.inappmessaging.FirebaseInAppMessagingContextualTrigger
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

@Suppress("UNUSED")
class GDFirebaseMessaging constructor(godot: Godot): GodotPlugin(godot) {

    private val context: Context = godot.requireContext()

    private val messaging = FirebaseInAppMessaging.getInstance()

    init {
        messaging.addClickListener {
            message, action ->
                val url = action.actionUrl
                val meta = message.campaignMetadata
        }
    }

    @UsedByGodot
    fun autoDataConnection(value: Boolean) {
        messaging.isAutomaticDataCollectionEnabled = value
    }

    @UsedByGodot
    fun suppressed(value: Boolean) {
        messaging.setMessagesSuppressed(value)
    }

    @UsedByGodot
    fun trigger(trigger: String) {
        messaging.triggerEvent(trigger)
    }

    @UsedByGodot
    fun configure(params: Dictionary) {
        val maxImageHeightWeight = params["maxImageHeightWeight"] as Float?
        val maxImageWidthWeight = params["maxImageWidthWeight"] as Float?
        val maxBodyHeightWeight = params["maxBodyHeightWeight"] as Float?
        val maxBodyWidthWeight = params["maxBodyWidthWeight"] as Float?

        val maxDialogHeightPx = params["maxDialogHeightPx"] as Int?
        val maxDialogWidthPx = params["maxDialogWidthPx"] as Int?
        val windowFlag = params["windowFlag"] as Int?
        val viewWindowGravity = params["viewWindowGravity"] as Int?
        val windowWidth = params["windowWidth"] as Int?
        val windowHeight = params["windowHeight"] as Int?

        val backgroundEnabled = params["backgroundEnabled"] as Boolean?
        val animate = params["animate"] as Boolean?
        val autoDismiss = params["autoDismiss"] as Boolean?
    }

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(
            SignalInfo("message_received", Dictionary::class.javaObjectType)
        )
    }

    override fun getPluginName(): String {
        return "GDFirebaseMessaging"
    }
}