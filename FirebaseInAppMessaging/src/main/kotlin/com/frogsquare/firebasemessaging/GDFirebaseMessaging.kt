package com.frogsquare.firebasemessaging

import android.content.Context
import com.frogsquare.firebase.Common
import com.frogsquare.firebase.Utils
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
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
        messaging.addClickListener { message, action ->
            val campaignId = message.campaignMetadata?.campaignId ?: ""
            emitSignal("action", action.actionUrl, campaignId, message.data)
        }
    }

    @UsedByGodot
    fun initialize(params: Dictionary) {
        if (params.containsKey("auto_data_connection")) {
            autoDataConnection(params["auto_data_connection"] as Boolean)
        }
        if (params.containsKey("messages_suppressed")) {
            suppressed(params["messages_suppressed"] as Boolean)
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
        runOnUiThread {
            messaging.triggerEvent(trigger)
        }
    }

    /*
     * maxImageHeightWeight : Float?
     * maxImageWidthWeight  : Float?
     * maxBodyHeightWeight  : Float?
     * maxBodyWidthWeight   : Float?
     * maxDialogHeightPx    : Int?
     * maxDialogWidthPx     : Int?
     * windowFlag           : Int?
     * viewWindowGravity    : Int?
     * windowWidth          : Int?
     * windowHeight         : Int?
     * backgroundEnabled    : Boolean?
     * animate              : Boolean?
     * autoDismiss          : Boolean?
     */

    @UsedByGodot
    fun configure(params: Dictionary) {
        val json = Utils.dictionaryToJson(params)
        val prefs = context.getSharedPreferences(Common.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("in_app_messaging_display_config", json.toString()).apply()
    }

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(
            SignalInfo("message_received", Dictionary::class.javaObjectType),
            SignalInfo("installation_id", String::class.javaObjectType),
            SignalInfo(
                "action",
                String::class.javaObjectType,
                String::class.javaObjectType,
                Dictionary::class.javaObjectType
            )
        )
    }

    override fun getPluginName(): String {
        return "GDFirebaseMessaging"
    }
}