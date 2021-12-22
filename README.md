# GDFirebase
	GDFirebase is a Modular Godot Plugin for using Firebase

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://github.com/FrogSquare/GDFirebase)
[![GodotEngine](https://img.shields.io/badge/Godot_Engine-3.3-blue.svg)](https://github.com/godotengine/godot)
[![LICENCE](https://img.shields.io/badge/License-Apache_V2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Depends on

> Godot game engine: `git clone https://github.com/godotengine/godot`

# Available Features

> AdMob

> Analytics

> Authentication `Google, Facebook, Anonymoys, Email`

> Cloud Messaging

> RemoteConfig

> Storage

# Getting Started
* Install Android build Template to your `GAME-PROJECT`
* Copy `google-services.json` you downloaded from Firebase to `[GAME-PROJECT]/android/build/`

# Installing
Add `google-services` plugin to your `[GAME-PROJECT]/android/build/build.gradle` file
```
    dependencies {
        classpath 'com.google.gms:google-services:4.3.10'
        // ...
    }
```
Now Apply the Google Services Plugin in your `build.gradle` file, like: 
```
apply plugin: 'com.google.gms.google-services'
```

Edit your `[GAME-PROJECT]/android/build/AndroidManifest.xml` file and add the Following inside the `<application>` tag

> If you are using `AdMob`

```
    <!-- AdMob -->
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="[APPLICATION_ID]"/>
```
Replace `[APPLICATION_ID]` with your AdMob application id.

> If you are using `Authentication (Facebook)`

```
    <!-- Facebook -->
    <meta-data android:name="com.facebook.sdk.ApplicationId"
        android:value="@string/facebook_app_id"/>

    <activity android:name="com.facebook.FacebookActivity"
        android:configChanges=
            "keyboard|keyboardHidden|screenLayout|screenSize|orientation"
        android:label="@string/godot_project_name_string" />
    <activity
        android:name="com.facebook.CustomTabActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="@string/fb_login_protocol_scheme" />
        </intent-filter>
    </activity>
```

Now Edit your `[GAME-PROJECT]/android/build/res/values/ids.xml` file and add

> If you are using `Authentication (Facebook)`

```
    <string name="facebook_app_id">[APPLICATION_ID]</string>
    <string name="fb_login_protocol_scheme">fb[APPLICATION_ID]</string>
```

#### Analytcis

```
    func _ready():
        if Engine.has_singleton("GDFirebase"):
            firebase = Engine.get_singleton("GDFirebase")
            firebase.initialize({dev_mode = true})
```
```
fun sendCustom(name: String, params: Dictionary)
fun sendScreenView(screenClass: String, screenName: String)

// Every param is an optional
fun sendAdImpression({
    value = Float,
    currency = String,
    source = String,
    platform = String,
    format = String,
    unitName = String
})
// Every param is an optional
fun sendPurchase({
    value = Float,
    currency = String,
    affiliation = String,
    coupon = String,
    shipping = String,
    tax = String,
    transactionId = String,
    items = [String]
})
// Every param is an optional
fun sendRefund({
    value = Float,
    currency = String,
    affiliation = String,
    coupon = String,
    shipping = String,
    tax = String,
    transactionId = String,
    items = [String]
})

fun sendJoinGroup(groupID: String)
fun sendSearch(item: String)
fun sendSelectContent(type: String, id: String)
fun sendShare(type: String, id: String, method: String)

fun sendLogin(method: String)
fun sendSignUp(method: String)

fun sendEarnVirtualCurrency(name: String, value: Double)
fun sendSpendVirtualCurrency(itemName: String, currencyName: String, value: Double)

fun sendTutorialBegin()
fun sendTutorialComplete()

fun sendLevelStart(name: String)
fun sendLevelEnd(name: String, {success = String}) // success is optional
fun sendLevelUp(level: Int, {character = String})  // character is optional

fun sendPostScore(score: Int, {level = Float, character = String}) // level and character is optional
fun sendUnlockAchievement(achievementId: String)

// name is optional, delay is in seconds
fun createNotification({
    name = String,
    title = String,   
    message = String,
    channelId = String,
    delay = Int,
})
fun createNotificationChannel({
    id = String,
    name = String
})
```

#### AdMob

    Banner and Interstitial Video is not implemented yet.

```
    func _ready():
        if Engine.has_singleton("GDFirebaseAdmob"):
            admob = Engine.get_singleton("GDFirebaseAdmob")
            admob.initialize({ ... })
```
For testing use the official ad-units priovided by admob

```
Interstitial            ca-app-pub-3940256099942544/1033173712
Rewarded                ca-app-pub-3940256099942544/5224354917
Rewarded Interstitial   ca-app-pub-3940256099942544/5354046379
```
```
fun initialize({
    test = Boolean,
    interstitialUnits = [String],
    rewardUnits = [String],
    rewardedInterstitialUnits = [String]
})

// Following will show a ramdom ad-unit (Only Loaded) if more that one present
fun showInterstitial()
fun showRewarded()
fun showRewardedInterstitial()

fun showInterstitialFor(unit: String)
fun showRewardedFor(unit: String)
fun showRewardedInterstitialFor(unit: String)

fun reloadAll()
fun reloadFor(type: String)

// Get if any ad-unit is Loaded
fun isLoaded(type: String) // type: rewarded, interstitial, rewarded_interstitial
fun isLoadedFor(type: String, unit: String) // type: rewarded, interstitial, rewarded_interstitial

// Signals
"loaded" -> loaded(type: Sting, unit: String)
"failed" -> failed(type: Sting, unit: String)
"showed" -> showed(type: Sting, unit: String)
"dismissed" -> dismissed(type: Sting, unit: String)
```

#### Authentication
```
    func _ready():
        if Engine.has_singleton("GDFirebaseAuth"):
            auth = Engine.get_singleton("GDFirebaseAuth")
            auth.initialize({ ... })
```
    Authentication providers will be sepearated into there own plugins in the upcomming updates

```
fun initialize({
    google = {},    // OPTIONAL
    facebook = {permissions = ["email", "public_profile"]},  // OPTIONAL, Default
})

// provider: google, facebook, anonymous, email
// params: Dictionary containing email and password, for email signIn.
fun signIn(provider: String, params: {
    email: String,
    password: String
})

fun createEmailAccount(email: String, password: String)
fun revokeAccess(provider: String)
fun signOut()
fun isConnected(): Boolean

// returns: Dictionary with (uid, name, email, photo, provider, is_anonymous, email_verified)
fun getUserInfo(): Dictionary

```

#### Remote Config

RemoteConfigs default parameters `.xml` file is at `[GAME-PROJECT]/android/build/res/xml/remote_config_defaults.xml`

```
    func _ready():
        if Engine.has_singleton("GDFirebaseConfig"):
            config = Engine.get_singleton("GDFirebaseConfig")
            config.fetch()

```
    Calling `fetch()` will activate the removeConfigs rightaway, [Read Mode.](https://firebase.google.com/docs/remote-config/loading)

```
fun setDefaults(json: String)
fun setDefaultsFile(jsonFilePath: String)
fun setDefaultsAsync(default: Dictionary)
fun fetch() 
fun fetchWith({activate = Boolean})
fun activate()

// FAILURE = 1
// NO_FETCH_YET = 0
// SUCCESS = -1
// THROTTLED = 2
fun getFetchStatus(): Int

fun getBoolean(key: String): Boolean
fun getFloat(key: String): key
fun getInt(key: String): Int
fun getString(key: String): String

fun getAll(): Dictionary

fun isReady(): Boolean
```

#### Storage
```
    func _ready():
        if Engine.has_singleton("GDFirebaseStorage"):
            storage = Engine.get_singleton("GDFirebaseStorage")
```

```
fun download(url: String, path: String)
fun upload(filePath: String, child: String)
```

#### Cloud Messaging
```
    func _ready():
        if Engine.has_singleton("GDFirebaseCloudMessaging"):
            cm = Engine.get_singleton("GDFirebaseCloudMessaging")
```
```
fun subscribe(topic: String)
fun unsubscribe(topic: String)
```
