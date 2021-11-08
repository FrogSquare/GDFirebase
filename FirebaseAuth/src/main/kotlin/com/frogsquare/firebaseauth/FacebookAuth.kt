package com.frogsquare.firebaseauth

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log

import org.godotengine.godot.Dictionary

import com.google.firebase.auth.FirebaseAuth

import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

import com.frogsquare.firebase.GDFirebase
import com.frogsquare.firebase.SingletonHolder

private const val TAG: String = "GDFirebaseAuth"

@Suppress("UNUSED", "UNUSED_PARAMETER")
class FacebookAuth private constructor(appContext: Context) {
    private val context: Context = appContext

    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth

    private lateinit var callbackManager: CallbackManager
    private lateinit var tokenTracker: AccessTokenTracker
    private lateinit var profileTracker: ProfileTracker
    private lateinit var callback: AuthCallback

    private var profile: Profile? = null
    private var accessToken: AccessToken? = null
    private var permissions = listOf("email", "public_profile")
    private var permissionsGranted = arrayListOf<String>()

    @Suppress("UNCHECKED_CAST")
    fun initialize(auth: FirebaseAuth, params: Dictionary, callbacks: AuthCallback?) {
        if (GDFirebase.isDevelopment) {
            Log.i(TAG, "Initializing google")
        }

        this.auth = auth
        if (callbacks != null)
            this.callback = callbacks

        this.callbackManager = CallbackManager.Factory.create()

        val permissions = params["permissions"] as List<String>?
        permissions?.let {
            this.permissions = it
        }

        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                onSignInComplete(result)
            }

            override fun onCancel() {
                Log.d(TAG, "Facebook::Login::Canceled")
            }

            override fun onError(error: FacebookException) {
                Log.d(TAG, "Facebook::Login::Failed::${error.message}")
            }
        })

        tokenTracker = object : AccessTokenTracker() {
            override fun onCurrentAccessTokenChanged(
                oldAccessToken: AccessToken?,
                currentAccessToken: AccessToken?
            ) {
                Log.d(TAG, "Facebook::AccessToken::changed")
                if (currentAccessToken == null) {
                    onSignOutComplete()
                } else {
                    accessToken = currentAccessToken
                }
            }
        }

        profileTracker = object : ProfileTracker() {
            override fun onCurrentProfileChanged(oldProfile: Profile?, currentProfile: Profile?) {
                Log.d(TAG, "Facebook::Profile::Changed")
                profile = currentProfile
            }
        }

        tokenTracker.startTracking()
        profileTracker.startTracking()

        accessToken = AccessToken.getCurrentAccessToken()
        profile = Profile.getCurrentProfile()
    }

    fun setActivity(activity: Activity) {
        this.activity = activity
    }

    fun isConnected(): Boolean {
        return profile != null
    }

    fun isPermissionGranted(permission: String): Boolean {
        val accessToken = AccessToken.getCurrentAccessToken()

        if(accessToken?.isExpired == true) {
            return false
        }

        return permissionsGranted.contains(permission)
    }

    fun signIn(activity: Activity) {
        if (!this::callbackManager.isInitialized) { return }
        requireReadPermissions(this.permissions)
    }

    fun signOut() {
        if (!this::callbackManager.isInitialized) { return }

        Log.d(TAG, "Facebook::Logout")
        LoginManager.getInstance().logOut()
    }

    fun revokeAccess() {
        val token = AccessToken.getCurrentAccessToken()
        val graph = GraphRequest.newDeleteObjectRequest(
            token,
            "me/permissions"
        ) {
            val error = it.error
            if (error == null) {
                Log.d(TAG, "Facebook::Access::Revoked")
            }
        }

        graph.executeAsync()
    }

    private fun getPermissions() {
        GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "me/permissions",
            null,
            HttpMethod.GET,
            {
                val data = it.getJSONObject()?.optJSONArray("data")
                data?.let {
                    permissionsGranted.clear()

                    for (i: Int in 0 until data.length()) {
                        val dd = data.optJSONObject(i)

                        if (dd.optString("status").equals("granted")) {
                            permissionsGranted.add(dd.optString("permission"))
                        }
                    }
                }
            }).executeAsync()
    }

    fun getUserDetails(): Map<String, Any?> {
        return mapOf(
            "name" to profile?.name,
            "first_name" to profile?.firstName,
            "last_name" to profile?.lastName,
            "photo" to profile?.pictureUri
        )
    }

    fun onSignInComplete(result: LoginResult) {
        accessToken = result.accessToken
        accessToken?.let {

            getPermissions()

            Log.d(TAG, "Facebook::SignIn::Success")
            callback.onSignedIn(it.token)
        }
    }

    fun onSignOutComplete() {
        Log.d(TAG, "Facebook::SignOut::Complete")
        callback.onSignedOut()
        profile = null
    }

    fun askForPermission(title: String, message: String, permission: String, read: Boolean) {
        AlertDialog.Builder(activity)
            .setPositiveButton("Yes"
            ) { _, _ ->
                if (!read) {
                    requirePublicPermissions(listOf(permission))
                } else {
                    requireReadPermissions(listOf(permission))
                }
            }
            .setNegativeButton("No") {
                _, _ ->
            }
            .setTitle(title)
            .setMessage(message)
            .show()
    }

    private fun requirePublicPermissions(permissions: Collection<String>) {
        if (!this::activity.isInitialized) {
            return
        }
        LoginManager.getInstance().logInWithPublishPermissions(activity, permissions)
    }

    private fun requireReadPermissions(permissions: Collection<String>) {
        if (!this::activity.isInitialized) {
            return
        }
        LoginManager.getInstance().logInWithReadPermissions(activity, permissions)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    companion object : SingletonHolder<FacebookAuth, Context>(::FacebookAuth)
}