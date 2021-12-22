package com.frogsquare.firebaseadmob

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*

import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import org.godotengine.godot.Dictionary

import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

private const val TAG: String = "GDFirebaseAdmob"

private const val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
private const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
private const val REWARDED_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/5354046379"

@Suppress("UNUSED")
class GDFirebaseAdmob constructor(godot: Godot): GodotPlugin(godot) {
    private val myActivity: Activity? = godot.activity
    private val myContext: Context? = godot.context

    private var mInitializationComplete: Boolean = false
    private var mInterstitialAds: HashMap<String, InterstitialAd> = hashMapOf()
    private var mRewardAds: HashMap<String, RewardedAd> = hashMapOf()
    private var mRewardedInterstitialAds: HashMap<String, RewardedInterstitialAd> = hashMapOf()

    private var autoReload: Boolean = true
    private var adUnits: Map<String, Array<String>?> = mapOf()

    init {
        MobileAds.initialize(myContext!!) {
            mInitializationComplete = true
        }
    }

    @UsedByGodot
    fun isAdapterInitialized(): Boolean {
        return mInitializationComplete
    }

    @UsedByGodot
    @Suppress( "UNCHECKED_CAST")
    fun initialize(params: Dictionary) {
        autoReload = params["auto_reload"] as Boolean? ?: true

        //val bannerAdUnits = params["bannerUnits"] as String?
        //var bannerPosition = params["bannerPosition"] as String? ?: "BOTTOM"
        val initTestAds = params["test"]
        val interstitialAdUnits = params["interstitialUnits"] as Array<String>?
        val rewardAdUnits = params["rewardUnits"] as Array<String>?
        val rewardedInterstitialAdUnits = params["rewardedInterstitialUnits"] as Array<String>?

        adUnits = mapOf(
            "interstitial" to interstitialAdUnits,
            "rewarded" to rewardAdUnits,
            "rewarded_interstitial" to rewardedInterstitialAdUnits
        )

        runOnUiThread {
            if (interstitialAdUnits.isNullOrEmpty()) {
                if (initTestAds != null) {
                    createInterstitial(INTERSTITIAL_ID)
                }
            } else {
                for (unit in interstitialAdUnits) {
                    createInterstitial(unit)
                }
            }

            if (rewardAdUnits.isNullOrEmpty()) {
                if (initTestAds != null) {
                    createRewarded(REWARDED_ID)
                }
            } else {
                for (unit in rewardAdUnits) {
                    createRewarded(unit)
                }
            }

            if (rewardedInterstitialAdUnits.isNullOrEmpty()) {
                if (initTestAds != null) {
                    createRewardedInterstitial(REWARDED_INTERSTITIAL_ID)
                }
            } else {
                for (unit in rewardedInterstitialAdUnits) {
                    createRewardedInterstitial(unit)
                }
            }
        }
    }

    private fun createInterstitial(unit: String) {
        if (mInterstitialAds.contains(unit)) {
            mInterstitialAds.remove(unit)
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            myContext!!,
            unit,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)

                    if (mInterstitialAds.contains(unit)) {
                        mInterstitialAds.remove(unit)
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    interstitialAd.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                            Log.d(TAG, "Ad failed to show.")
                            emitSignal(
                                "failed",
                                "interstitial",
                                interstitialAd.adUnitId
                            )
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Ad was dismissed.")
                            emitSignal(
                                "dismissed",
                                "interstitial",
                                interstitialAd.adUnitId
                            )
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Ad showed fullscreen content.")
                            emitSignal(
                                "showed",
                                "interstitial",
                                interstitialAd.adUnitId
                            )
                            if (autoReload) createInterstitial(unit)
                        }
                    }

                    emitSignal(
                        "loaded",
                        "interstitial",
                        interstitialAd.adUnitId
                    )
                    mInterstitialAds[unit] = interstitialAd
                }
            }
        )
    }

    private fun createRewarded(unit: String) {
        if (mRewardAds.contains(unit)) {
            mRewardAds.remove(unit)
        }

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(myContext!!, unit, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(p0: LoadAdError) {
                Log.d(TAG, "Ad Failed to Load")
                if (mRewardAds.contains(unit)) {
                    mRewardAds.remove(unit)
                }
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Log.d(TAG, "Ad Loaded.")

                rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        Log.d(TAG, "Ad Failed to Shown.")
                        emitSignal(
                            "failed",
                            "rewarded",
                            rewardedAd.adUnitId
                        )
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad Dismissed.")
                        emitSignal(
                            "dismissed",
                            "rewarded",
                            rewardedAd.adUnitId
                        )
                    }
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad Was Shown.")
                        emitSignal(
                            "showed",
                            "rewarded",
                            rewardedAd.adUnitId
                        )
                        if (autoReload) createRewarded(unit)
                    }
                }
                emitSignal("loaded", "rewarded", rewardedAd.adUnitId)
                mRewardAds[unit] = rewardedAd
            }
        })
    }

    private fun createRewardedInterstitial(unit: String) {
        if (mRewardedInterstitialAds.contains(unit)) {
            mRewardedInterstitialAds.remove(unit)
        }

        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(
            myContext!!,
            "unit",
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(rewardedInterstitialAd: RewardedInterstitialAd) {
                Log.d(TAG, "Ad Loaded.")

                rewardedInterstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        Log.d(TAG, "Ad Failed to Shown.")
                        emitSignal(
                            "failed",
                            "rewarded_interstitial",
                            rewardedInterstitialAd.adUnitId
                        )
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad Dismissed.")
                        emitSignal(
                            "dismissed",
                            "rewarded_interstitial",
                            rewardedInterstitialAd.adUnitId
                        )
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad Was Shown.")
                        emitSignal(
                            "showed",
                            "rewarded_interstitial",
                            rewardedInterstitialAd.adUnitId
                        )
                        if (autoReload) createRewardedInterstitial(unit)
                    }
                }

                emitSignal(
                    "loaded",
                    "rewarded_interstitial",
                    rewardedInterstitialAd.adUnitId
                )
                mRewardedInterstitialAds[unit] = rewardedInterstitialAd
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                Log.d(TAG, "Ad Failed to Load")

                if (mRewardedInterstitialAds.containsKey(unit)) {
                    mRewardedInterstitialAds.remove(unit)
                }
            }
        })
    }

    @UsedByGodot
    fun reloadAll() {
        reloadFor("interstitial")
        reloadFor("rewarded")
        reloadFor("rewarded_interstitial")
    }

    @UsedByGodot
    fun reloadFor(type: String) {
        val units = adUnits[type]

        if (units == null) {
            Log.d(TAG, "Ad Units for $type not available")
            return
        }

        runOnUiThread {
            units.forEach { unit ->
                when (type) {
                    "interstitial" -> createInterstitial(unit)
                    "rewarded" -> createRewarded(unit)
                    "rewarded_interstitial" -> createRewardedInterstitial(unit)
                }
            }
        }
    }

    @UsedByGodot
    fun showInterstitial() {
        val unit = mInterstitialAds.keys.randomOrNull()
        if (unit != null) {
            showInterstitialFor(unit)
        }
    }

    @UsedByGodot
    fun showRewarded() {
        val unit = mRewardAds.keys.randomOrNull()
        if (unit != null) {
            showRewardedFor(unit)
        }
    }

    @UsedByGodot
    fun showRewardedInterstitial() {
        val unit = mRewardedInterstitialAds.keys.randomOrNull()
        if (unit != null) {
            showRewardedInterstitialFor(unit)
        }
    }

    @UsedByGodot
    fun showInterstitialFor(unit: String) {
        if (!mInterstitialAds.containsKey(unit)) return

        val ad = mInterstitialAds[unit]
        ad?.let {
            runOnUiThread {
                it.show(myActivity!!)
            }
        }
    }

    @UsedByGodot
    fun showRewardedFor(unit: String) {
        if (!mRewardAds.containsKey(unit)) return

        val rewardedAd = mRewardAds[unit]
        rewardedAd?.let {ad ->
            runOnUiThread {
                ad.show(myActivity!!) {
                    val dict = Dictionary()
                    dict["name"] = it.type
                    dict["amount"] = it.amount

                    emitSignal("rewarded", dict)
                    createRewarded(unit)
                }
            }
        }
    }

    @UsedByGodot
    fun showRewardedInterstitialFor(unit: String) {
        if (!mRewardedInterstitialAds.containsKey(unit)) return
        val rewardedInterstitialAd = mRewardedInterstitialAds[unit]
        rewardedInterstitialAd?.let { ad ->
            runOnUiThread {
                ad.show(myActivity!!) {
                    val dict = Dictionary()
                    dict["name"] = it.type
                    dict["amount"] = it.amount

                    emitSignal("rewarded", dict)
                    createRewardedInterstitial(unit)
                }
            }
        }
    }

    @UsedByGodot
    fun isLoaded(type: String): Boolean {
        return when (type) {
            "rewarded" -> mRewardAds.isNotEmpty()
            "interstitial" -> mInterstitialAds.isNotEmpty()
            "rewarded_interstitial" -> mRewardedInterstitialAds.isNotEmpty()
            else -> false
        }
    }

    @UsedByGodot
    fun isLoadedFor(type: String, unit: String): Boolean {
        return when (type) {
            "rewarded" -> mRewardAds.containsKey(unit)
            "interstitial" -> mInterstitialAds.containsKey(unit)
            "rewarded_interstitial" -> mRewardedInterstitialAds.containsKey(unit)
            else -> false
        }
    }

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(
            SignalInfo("failed", String::class.javaObjectType, String::class.javaObjectType),
            SignalInfo("showed", String::class.javaObjectType, String::class.javaObjectType),
            SignalInfo("dismissed", String::class.javaObjectType, String::class.javaObjectType),
            SignalInfo("loaded", String::class.javaObjectType, String::class.javaObjectType),
            SignalInfo("rewarded", Dictionary::class.javaObjectType)
        )
    }

    override fun getPluginName(): String {
        return "GDFirebaseAdmob"
    }
}