package com.frogsquare.firebaseauth

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.frogsquare.firebase.GDFirebase
import com.frogsquare.firebase.Utils
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.*

import com.google.firebase.auth.ktx.auth
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin

import com.google.firebase.ktx.Firebase
import org.godotengine.godot.Dictionary
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

abstract interface AuthCallback {
   open fun onSignedIn(token: String) {}
    open fun onSignedOut() {}
}

const val RC_GOOGLE_ID = 0x0004
const val RC_FACEBOOK_ID = 0x0005

private const val TAG: String = "GDFirebaseAuth"

@Suppress("UNUSED")
class GDFirebaseAuth constructor(godot: Godot): GodotPlugin(godot) {
    private val auth: FirebaseAuth = Firebase.auth
    private val context: Context = godot.requireContext()

    private var token: String? = null
    private var isInDevMode: Boolean = false

    private var userDetails: Dictionary = Dictionary()

    companion object {
        var currentUser: FirebaseUser? = null

        val isSignedIn: Boolean
        get () {
            return currentUser != null
        }
    }

    init {
        val user = auth.currentUser
        user?.let {
            userSignedIn(it)
        }
    }

    @UsedByGodot
    fun initialize(params: Dictionary) {
        Log.i(TAG, "Initializing! ${params}")
        isInDevMode = GDFirebase.isDevelopment

        val google = params["google"] as Dictionary?
        val facebook = params["facebook"] as Dictionary?

        if (!google.isNullOrEmpty()) {
            GoogleAuth.getInstance(context).initialize(auth, google, object : AuthCallback {
                override fun onSignedIn(token: String) {
                    signInGoogle(token)
                }

                override fun onSignedOut() {
                    emitSignal("signed_out", "google")
                }
            })
        }
        if (!facebook.isNullOrEmpty()) {
            FacebookAuth.getInstance(context).initialize(auth, facebook, object : AuthCallback {
                override fun onSignedIn(token: String) {
                    signInFacebook(token)
                }

                override fun onSignedOut() {
                    super.onSignedOut()
                    emitSignal("signed_out", "facebook")
                }
            })
        }
    }

    @UsedByGodot
    fun signIn(method: String, params: Dictionary) {
        Log.i(TAG, "Signing in to $method")

        when (method.lowercase()) {
            "google" -> GoogleAuth.getInstance(context).signIn(activity!!)
            "facebook" -> FacebookAuth.getInstance(context).signIn(activity!!)
            "anonymous" -> {
                auth.signInAnonymously().addOnCompleteListener {
                    if (it.isSuccessful) {
                        val user = auth.currentUser
                        user?.let { usr ->
                            userSignedIn(usr)
                        }
                    }
                }
            }
            "email" -> {
                val email = params["email"] as String?
                val password = params["password"] as String?

                if (email != null && password != null) {
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                        if (it.isSuccessful) {
                            val user = auth.currentUser
                            user?.let { usr ->
                                userSignedIn(usr)
                            }
                        }
                    }
                }
            }
            else -> {
                Log.e(TAG, "Login method `$method` not implemented.")
            }
        }
    }

    @UsedByGodot
    fun signOut() {
        auth.signOut()
    }

    @UsedByGodot
    fun isConnected(method: String): Boolean {
        return when (method) {
            "google" -> GoogleAuth.getInstance(context).isConnected()
            "facebook" -> FacebookAuth.getInstance(context).isConnected()
            else -> false
        }
    }

    @UsedByGodot
    fun revokeAccess(method: String) {
        if (isInDevMode) {
            val applicationName = Utils.getApplicationName(context)
            Log.i(TAG, "Revoke $applicationName access to $method")
        }

        when (method.lowercase()) {
            "google" -> GoogleAuth.getInstance(context).revokeAccess()
            "facebook" -> FacebookAuth.getInstance(context).revokeAccess()
            "anonymous", "email" -> {}
            else -> {
                Log.e(TAG, "Remove method `$method` not implemented.")
            }
        }
        signOutFrom(method)
    }

    @UsedByGodot
    fun getUserInfo(): Dictionary {
        return userDetails
    }

    @UsedByGodot
    fun getUserToken() {
        val user = auth.currentUser
        if (user != null) {
            loadUserToken(user)
        }
    }

    @UsedByGodot
    fun createEmailAccount(email: String, password: String) {
        Log.d(TAG, "createAccount::For::$email")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity!!) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail::success")
                    val user = auth.currentUser
                    user?.let {
                        userSignedIn(it)
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail::failure", task.exception)
                    Toast.makeText(
                        context,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun signOutFrom(method: String) {
        when (method.lowercase()) {
            "google" -> GoogleAuth.getInstance(context).signOut()
            "facebook" -> FacebookAuth.getInstance(context).signOut()
            else -> {
                Log.e(TAG, "Remove method `$method` not implemented.")
            }
        }
    }

    private fun userSignedIn(user: FirebaseUser) {
        Log.i(TAG, "User `${user.displayName}` Signed in.")

        userDetails["uid"] = user.uid
        userDetails["name"] = user.displayName
        userDetails["email"] = user.email
        userDetails["photo"] = user.photoUrl
        userDetails["provider"] = user.providerId
        userDetails["is_anonymous"] = user.isAnonymous
        userDetails["email_verified"] = user.isEmailVerified

        loadUserToken(user)
        currentUser = user

        emitSignal("signed_in", user.providerId)
    }

    private fun loadUserToken(user: FirebaseUser) {
        user.getIdToken(true).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                token = task.result.token
                emitSignal("token", token)
            } else {
                emitSignal("token", null)
            }
        }
    }

    private fun signInAnonymous() {
        auth.signInAnonymously().addOnCompleteListener {
            if (it.isSuccessful) {
                val user = it.result?.user
                user?.let { usr ->
                    userSignedIn(usr)
                }
            }
        }
    }

    private fun signInGoogle(token: String) {
        val user = auth.currentUser
        val credential = GoogleAuthProvider.getCredential(token, token)
        val listener =  OnCompleteListener<AuthResult> {
            if (it.isSuccessful) {
                Log.i(TAG, "Firebase connected with Google")
                emitSignal("signed_in", "google")
            }
        }

        if (user?.isAnonymous == true) {
            user.linkWithCredential(credential).addOnCompleteListener(listener)
        } else {
            auth.signInWithCredential(credential).addOnCompleteListener(listener)
        }
    }

    private fun signInFacebook(token: String) {
        val credential = FacebookAuthProvider.getCredential(token)
        val user = auth.currentUser

        val listener =  OnCompleteListener<AuthResult> {
            if (it.isSuccessful) {
                Log.i(TAG, "Firebase connected with Facebook")
                emitSignal("signed_in", "facebook")
            }
        }

        if (user?.isAnonymous == true) {
            user.linkWithCredential(credential).addOnCompleteListener(listener)
        } else {
            auth.signInWithCredential(credential).addOnCompleteListener(listener)
        }
    }

    override fun onMainActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_FACEBOOK_ID) {
            FacebookAuth.getInstance(context).onActivityResult(requestCode, resultCode,data)
            return
        }

        if (requestCode == RC_GOOGLE_ID) {
            GoogleAuth.getInstance(context).onActivityResult(requestCode, resultCode,data)
            return
        }

        super.onMainActivityResult(requestCode, resultCode, data)
    }

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(
            SignalInfo("login_request_complete", Boolean::class.javaObjectType),
            SignalInfo("token", String::class.javaObjectType),

            SignalInfo("signed_in", String::class.javaObjectType),
            SignalInfo("signed_out", String::class.javaObjectType)
        )
    }

    override fun getPluginName(): String {
        return "GDFirebaseAuth"
    }
}
