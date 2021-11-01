package com.frogsquare.firebaseconfig

import android.content.Context
import android.util.Log
import com.frogsquare.firebase.GDFirebase

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.google.firebase.remoteconfig.ktx.remoteConfig
import org.godotengine.godot.Dictionary

import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

private const val TAG: String = "GDFirebaseConfig"

@Suppress("UNUSED")
class GDFirebaseConfig constructor(godot: Godot) : GodotPlugin(godot) {

    private val context: Context? = godot.context
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()

        remoteConfig.setConfigSettingsAsync(settings)

        if (context != null) {
            val resourceID = context.resources.getIdentifier(
                "remote_config_defaults",
                "xml",
                context.packageName
            )

            if (resourceID != 0) {
                remoteConfig.setDefaultsAsync(resourceID)
            }
        }
    }

    @UsedByGodot
    fun setDefaultsAsync(defaults: Dictionary) {
        remoteConfig.setDefaultsAsync(defaults)
    }

    @UsedByGodot
    fun fetch() {
        fetchRemoveConfigs(true)
    }

    @UsedByGodot
    fun fetchWith(params: Dictionary) {
        val activate = params["activate"] as Boolean? ?: true
        fetchRemoveConfigs(activate)
    }

    @UsedByGodot
    fun activate() {
        remoteConfig.activate()
    }

    @UsedByGodot
    fun getFetchStatus(): Int {
        val info = remoteConfig.info
        return info.lastFetchStatus
    }

    @UsedByGodot
    fun getBoolean(key: String): Boolean {
        return getValue(key).asBoolean()
    }

    @UsedByGodot
    fun getFloat(key: String): Float {
        return getValue(key).asDouble().toFloat()
    }

    @UsedByGodot
    fun getInt(key: String): Int {
        return getValue(key).asLong().toInt()
    }

    @UsedByGodot
    fun getString(key: String): String {
        return getValue(key).asString()
    }

    @UsedByGodot
    fun getAll(): Dictionary {
        val ret = Dictionary()
        remoteConfig.all.forEach {
            ret[it.key] = it.value
        }
        return ret
    }

    @UsedByGodot
    fun isReady(): Boolean {
        val devMode = GDFirebase.isDevelopment

        return when (remoteConfig.info.lastFetchStatus) {
            FirebaseRemoteConfig.LAST_FETCH_STATUS_FAILURE -> {
                if (devMode)
                    Log.i(TAG, "Remove config fetch failed.")

                false
            }
            FirebaseRemoteConfig.LAST_FETCH_STATUS_NO_FETCH_YET -> {
                if (devMode)
                    Log.i(TAG, "Remove config is not yet been fetched, call `fetch()`.")

                false
            }
            FirebaseRemoteConfig.LAST_FETCH_STATUS_THROTTLED -> {
                if (devMode)
                    Log.i(TAG, "Remove config fetching Throttled.")

                false
            }
            FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS -> {
                true
            }
            else -> false
        }
    }

    private fun getValue(key: String): FirebaseRemoteConfigValue {
        val ret = remoteConfig.getValue(key)

        if (GDFirebase.isDevelopment) {
            when (ret.source) {
                FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT -> {
                    Log.i(TAG, "Value for $key returned from the defaults.")
                }
                FirebaseRemoteConfig.VALUE_SOURCE_REMOTE -> {
                    Log.i(TAG, "Value for $key returned from the Firebase Remote Config Server.")
                }
                FirebaseRemoteConfig.VALUE_SOURCE_STATIC -> {
                    Log.i(TAG, "Value for $key is the Static default value.")
                }
                else -> {}
            }
        }

        return ret
    }

    private fun fetchRemoveConfigs(activate: Boolean) {
        Log.i(TAG, "Fetching Remove config.")
        remoteConfig.fetch().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i(TAG, "Remote config, Fetch Success.")

                if (activate) {
                    remoteConfig.activate()
                }

                emitSignal("fetch_complete", true)
            } else {
                Log.i(TAG, "Remove config, Fetch Failed.")
                emitSignal("fetch_complete", false)
            }
        }
    }

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(
            SignalInfo("fetch_complete", Boolean::class.javaObjectType)
        )
    }

    override fun getPluginName(): String {
        return "GDFirebaseConfig"
    }
}