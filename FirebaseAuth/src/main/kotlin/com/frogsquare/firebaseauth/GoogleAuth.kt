package com.frogsquare.firebaseauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.frogsquare.firebase.Common
import com.frogsquare.firebase.GDFirebase
import com.frogsquare.firebase.SingletonHolder
import com.frogsquare.firebase.Utils

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import org.godotengine.godot.Dictionary

private const val TAG: String = "GDFirebaseAuth"

@Suppress("UNUSED")
class GoogleAuth private constructor(appContext: Context) {
    private val context: Context = appContext

    private lateinit var auth: FirebaseAuth
    private lateinit var client: GoogleSignInClient
    private lateinit var callback: AuthCallback

    private var account: GoogleSignInAccount? = null

    fun initialize(auth: FirebaseAuth, params: Dictionary, callbacks: AuthCallback?) {
        if (GDFirebase.isDevelopment) {
            Log.i(TAG, "Initializing google")
        }

        this.auth = auth
        if (callbacks != null)
            callback = callbacks

        val webClientId = Utils.getResourceID(context, "default_web_client_id", "string")
        val clientID =  if (webClientId <= 0 || params["clientId"] != null) {
            params["clientId"] as String
        } else {
            context.getString(webClientId)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            .requestIdToken(clientID)
            .requestEmail()
            .build()

        client = GoogleSignIn.getClient(context, gso)
        account = GoogleSignIn.getLastSignedInAccount(context)

        account?.let {
            it.idToken?.let { token ->
                callback.onSignedIn(token)
            }
        }
    }

    fun isConnected(): Boolean {
        return account != null
    }

    fun signIn(activity: Activity) {
        val intent = client.signInIntent
        activity.startActivityForResult(intent, Common.RC_GOOGLE)
    }

    fun signOut() {
        client.signOut()
        account = null
    }

    fun revokeAccess() {
        if (!this::client.isInitialized) return

        client.revokeAccess().addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "Google Access Revoked")
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == Common.RC_GOOGLE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                handleSignInResult(task)
            } else {
                val e = task.exception
                Log.d(TAG, "Google SignIn failed: $e\n" + Log.getStackTraceString(e))
            }
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            // Google Sign In was successful, authenticate with Firebase
            account = completedTask.getResult(ApiException::class.java)!!
            callback.onSignedIn(account?.idToken!!)
        } catch (e: ApiException) {
            // Google Sign In failed, update UI appropriately
            Log.w(TAG, "Google sign in failed ${e.message}\n" + Log.getStackTraceString(e))
        }
    }

    companion object : SingletonHolder<GoogleAuth, Context>(::GoogleAuth)
}