package com.frogsquare.firebase

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.work.*

import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot
import org.godotengine.godot.Dictionary

import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Event.*
import com.google.firebase.analytics.FirebaseAnalytics.Param.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import java.util.concurrent.TimeUnit

private const val TAG: String = "GDFirebase"

@Suppress("UNUSED")
class GDFirebase constructor(godot: Godot): GodotPlugin(godot) {
    private val myActivity: Activity = godot.requireActivity()
    private val myContext: Context = godot.requireContext()

    private lateinit var firebaseApp: FirebaseApp
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    companion object {
        private var preferences: SharedPreferences? = null

        val isDevelopment: Boolean get() {
            if (preferences != null) {
                preferences!!.getBoolean("dev_mode", false)
            }
            return false
        }
    }

    init {
        preferences = myContext.getSharedPreferences(
            Utils.SHARED_PREFERENCE_NAME,
            Context.MODE_PRIVATE
        )

        val manager = WorkManager.getInstance(myContext)
        manager.cancelAllWork()
    }

    @UsedByGodot
    fun initialize(params: Dictionary): Boolean {
        Log.i(TAG, "Initializing Godot Firebase")

        try {
            firebaseApp = Firebase.app
            firebaseAnalytics = Firebase.analytics
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Initialization failed, ${e.message}\n" + Log.getStackTraceString(e))
            return false
        }

        Utils.loadPreferences(myContext, params)
        return true
    }

    @UsedByGodot
    fun sendCustom(name: String, params: Dictionary) {
        firebaseAnalytics.logEvent(name) {
            for (pair in params) {
                param(pair.key.toString(), pair.value.toString())
            }
        }
    }

    @UsedByGodot
    fun appOpen() {
        firebaseAnalytics.logEvent(APP_OPEN, null)
    }

    @UsedByGodot
    fun sendScreenView(screenClass: String, screenName: String) {
        firebaseAnalytics.logEvent(SCREEN_VIEW) {
            param(SCREEN_CLASS, screenClass)
            param(SCREEN_NAME, screenName)
        }
    }

    @UsedByGodot
    fun sendAdImpression(params: Dictionary) {
        val value = params["value"] as Float?
        val currency = params["currency"] as String?

        if (value != null && currency == null) {
            Log.e(TAG, "Currency cannot be null when sending Param#VALUE")
            return
        }

        val source = params["source"] as String?
        val platform = params["platform"] as String?
        val format = params["format"] as String?
        val unitName = params["unitName"] as String?

        firebaseAnalytics.logEvent(AD_IMPRESSION) {
            if (platform != null) param(AD_PLATFORM, platform)
            if (source != null) param(AD_SOURCE, source)
            if (format != null) param(AD_FORMAT, format)
            if (unitName != null) param(AD_UNIT_NAME, unitName)
            if (currency != null) param(CURRENCY, currency)
            if (value != null) param(VALUE, value.toDouble())
        }
    }

    @UsedByGodot
    fun sendEarnVirtualCurrency(name: String, value: Double) {
        firebaseAnalytics.logEvent(EARN_VIRTUAL_CURRENCY) {
            param(VIRTUAL_CURRENCY_NAME, name)
            param(VALUE, value)
        }
    }

    @UsedByGodot
    fun sendPurchase(params: Dictionary) {
        transaction(PURCHASE, params)
    }

    @UsedByGodot
    fun sendRefund(params: Dictionary) {
        transaction(REFUND, params)
    }

    @UsedByGodot
    fun sendJoinGroup(groupID: String) {
        firebaseAnalytics.logEvent(JOIN_GROUP) {
            param(GROUP_ID, groupID)
        }
    }

    @UsedByGodot
    fun sendLogin(method: String) {
        firebaseAnalytics.logEvent(LOGIN) {
            param(METHOD, method)
        }
    }

    @UsedByGodot
    fun sendSearch(item: String) {
        firebaseAnalytics.logEvent(SEARCH) {
            param(SEARCH_TERM, item)
        }
    }

    @UsedByGodot
    fun sendSelectContent(type: String, id: String) {
        firebaseAnalytics.logEvent(SELECT_CONTENT) {
            param(CONTENT_TYPE, type)
            param(ITEM_ID, id)
        }
    }

    @UsedByGodot
    fun sendShare(type: String, id: String, method: String) {
        firebaseAnalytics.logEvent(SHARE) {
            param(CONTENT_TYPE, type)
            param(ITEM_ID, id)
            param(METHOD, method)
        }
    }

    @UsedByGodot
    fun sendSignUp(method: String) {
        firebaseAnalytics.logEvent(SIGN_UP) {
            param(METHOD, method)
        }
    }

    @UsedByGodot
    fun sendSpendVirtualCurrency(itemName: String, currencyName: String, value: Double) {
        firebaseAnalytics.logEvent(SPEND_VIRTUAL_CURRENCY) {
            param(ITEM_NAME, itemName)
            param(VIRTUAL_CURRENCY_NAME, currencyName)
            param(VALUE, value)
        }
    }

    @UsedByGodot
    fun sendTutorialBegin() {
        firebaseAnalytics.logEvent(TUTORIAL_BEGIN, null)
    }

    @UsedByGodot
    fun sendTutorialComplete() {
        firebaseAnalytics.logEvent(TUTORIAL_COMPLETE, null)
    }

    @UsedByGodot
    fun sendLevelEnd(name: String, params: Dictionary) {
        val success = params["success"] as String?

        firebaseAnalytics.logEvent(LEVEL_END) {
            param(LEVEL_NAME, name)
            if (success != null) param(SUCCESS, success)
        }
    }

    @UsedByGodot
    fun sendLevelStart(name: String) {
        firebaseAnalytics.logEvent(LEVEL_START) {
            param(LEVEL_NAME, name)
        }
    }

    @UsedByGodot
    fun sendLevelUp(level: Int, params: Dictionary) {
        val character = params["character"] as String?
        firebaseAnalytics.logEvent(LEVEL_UP) {
            param(LEVEL, level.toLong())
            if (character != null) param(CHARACTER, character)
        }
    }

    @UsedByGodot
    fun sendPostScore(score: Int, params: Dictionary) {
        val level = params["level"] as Float?
        val character = params["character"] as String?

        firebaseAnalytics.logEvent(POST_SCORE) {
            param(SCORE, score.toLong())
            if (level != null) param(LEVEL, level.toLong())
            if (character != null) param(CHARACTER, character)
        }
    }

    @UsedByGodot
    fun sendUnlockAchievement(achievementId: String) {
        firebaseAnalytics.logEvent(UNLOCK_ACHIEVEMENT) {
            param(ACHIEVEMENT_ID, achievementId)
        }
    }

    @UsedByGodot
    fun createNotification(params: Dictionary) {
        val delay: Int = params["delay"] as Int? ?: 0

        val manager = WorkManager.getInstance(myContext)
        val work = OneTimeWorkRequestBuilder<NotifyInTime>()
            .setInputData(workDataOf(
                "title" to params["title"],
                "message" to params["message"],
                "channelId" to params["channelId"]
            ))
            .setInitialDelay(delay.toLong(), TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30000, TimeUnit.MILLISECONDS)

        val name = params["name"] as String?
        if (name != null) {
            manager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, work.build())
        } else {
            manager.enqueue(work.build())
        }
    }

    @UsedByGodot
    fun createNotificationChannel(params: Dictionary) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                myContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val id = params["id"] as String
            val name = params["name"] as String

            val channel = NotificationChannel(
                id,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun transaction(event: String, params: Dictionary) {
        assert(event == REFUND || event == PURCHASE)
        val value = params["value"] as Float?
        val currency = params["currency"] as String?

        if (value != null && currency == null) {
            Log.e(TAG, "Currency cannot be null when sending Param#VALUE")
            return
        }

        val affiliation = params["affiliation"] as String?
        val coupon = params["coupon"] as String?
        val shipping = params["shipping"] as String?
        val tax = params["tax"] as String?
        val transactionId = params["transactionId"] as String?

        @Suppress("UNCHECKED_CAST")
        val items = params["items"] as Array<String>?

        firebaseAnalytics.logEvent(REFUND) {
            if (affiliation != null) param(AFFILIATION, affiliation)
            if (currency != null) param(CURRENCY, currency)
            if (coupon != null) param(COUPON, coupon)
            if (shipping != null) param(SHIPPING, shipping)
            if (items != null) {
                val bundle = Bundle()
                bundle.putStringArray("items", items)

                param(ITEMS, bundle)
            }
            if (tax != null) param(TAX, tax)
            if (transactionId != null) param(TRANSACTION_ID, transactionId)
            if (value != null) param(VALUE, value.toDouble())
        }
    }

    override fun getPluginName(): String {
        return "GDFirebase"
    }
}
