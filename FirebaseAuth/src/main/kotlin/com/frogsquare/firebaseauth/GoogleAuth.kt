package com.frogsquare.firebaseauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
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
import java.util.*

private const val TAG: String = "GDFirebaseAuth"

@Suppress("UNUSED", "UNUSED_PARAMETER")
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
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            .requestIdToken(context.getString(webClientId))
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
        activity.startActivityForResult(intent, RC_GOOGLE_ID)
    }

    fun signOut() {
        client.signOut()
        account = null
    }

    fun revokeAccess() {

    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_GOOGLE_ID) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
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