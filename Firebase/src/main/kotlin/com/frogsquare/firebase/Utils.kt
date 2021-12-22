package com.frogsquare.firebase

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Build

import org.godotengine.godot.Dictionary
import org.json.JSONException
import org.json.JSONObject

import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.io.*


@Suppress("UNUSED")
object Utils {
    @JvmStatic
    fun getXmlID(context: Context, name: String): Int {
        return getResourceID(context, name, "xml")
    }

    @JvmStatic
    fun getMipmapID(context: Context, name: String): Int {
        return getResourceID(context, name, "mipmap")
    }

    @JvmStatic
    fun getResourceID(context: Context, name: String, defType: String): Int {
        return context.resources.getIdentifier(
            name,
            defType,
            context.packageName
        )
    }

    @JvmStatic
    fun getApplicationName(context: Context): String {
        val labelId = getResourceID(context, "godot_project_name_string", "string")
        return context.getString(labelId)
    }

    @JvmStatic
    @SuppressLint("UnspecifiedImmutableFlag")
    fun getMainIntent(context: Context): PendingIntent? {
        val cls = try {
            Class.forName("com.godot.game.GodotApp")
        } catch (e: ClassNotFoundException) {
            Log.e("GDFirebase", "${e.message}\n" + Log.getStackTraceString(e))
            return null
        }

        val intent = Intent(context, cls).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            Common.INTENT_REQUEST_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @JvmStatic
    fun getBitmapFromAsset(context: Context, fPath: String): Bitmap? {
        val filePath = if (fPath.startsWith("res://")) {
            fPath.replaceFirst("res://", "")
        } else {
            fPath
        }

        val assetManager = context.assets
        val inputStream: InputStream
        var bitmap: Bitmap? = null

        try {
            inputStream = assetManager.open(filePath)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            // handle exception
            Log.i("GDFirebase", "Bitmap decode failed.")
        }

        return bitmap
    }

    @JvmStatic
    fun doInBackground(imageUrl: String): Bitmap? {
        val inputStream: InputStream
        try {
            val url = URL(imageUrl)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()

            inputStream = connection.inputStream
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: MalformedURLException) {
            Log.e("GDFirebase", "${e.message}\n" + Log.getStackTraceString(e))
        } catch (e: IOException) {
            Log.e("GDFirebase", "${e.message}\n" + Log.getStackTraceString(e))
        }

        return null
    }

    @JvmStatic
    fun loadPreferences(context: Context, params: Dictionary) {
        val prefs = context.getSharedPreferences(
            Common.SHARED_PREFERENCE_NAME,
            Context.MODE_PRIVATE
        )
        val prefsEditor = prefs.edit()

        for (pair in params) {
            when (pair.value.javaClass) {
                Boolean::class.java -> {
                    prefsEditor.putBoolean(pair.key, pair.value as Boolean)
                }
                Int::class.java,
                Long::class.java -> {
                    prefsEditor.putInt(pair.key, pair.value as Int)
                }
                Float::class.java,
                Double::class.java -> {
                    prefsEditor.putFloat(pair.key, pair.value as Float)
                }
                String::class.java -> {
                    prefsEditor.putString(pair.key, pair.value as String)
                }
                else -> {
                    // do nothing
                }
            }
        }

        prefsEditor.apply()
    }

    @JvmStatic
    fun createDefaultChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Common.DEFAULT_CHANNEL_ID,
                "Default",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            manager.createNotificationChannel(channel)
        }
    }

    @JvmStatic
    fun jsonToDictionary(jsonString: String): Dictionary {
        val ret = Dictionary()
        try {
            val json = JSONObject(jsonString)
            val iterator = json.keys().iterator()

            while (iterator.hasNext()) {
                val key = iterator.next()
                val value = json.get(key)

                ret[key] = value
            }
        } catch (e: JSONException) {
            Log.d("GDFirebase", "JSONException ${e.message}/n"+Log.getStackTraceString(e))
        }
        return ret
    }

    @JvmStatic
    fun dictionaryToJson(data: Dictionary): JSONObject {
        val json = JSONObject()

        try {
            for (pair in data) {
                json.put(pair.key, pair.value)
            }
        } catch (e: JSONException) {
            Log.e("GDFirebase", "JSON Exception ${e.message}\n"+Log.getStackTraceString(e))
        }

        return json
    }

    @JvmStatic
    fun readFromFile(context: Context, path: String): String {
        var filepath = path
        if (filepath.startsWith("res://")) {
            filepath = filepath.replaceFirst("res://", "")
        }

        val ret = StringBuilder()
        var fIn: InputStream? = null
        var isr : InputStreamReader? = null
        var input : BufferedReader? = null

        try {
            fIn = context.resources.assets.open(filepath, Context.MODE_PRIVATE)
            isr = InputStreamReader(fIn)
            input = BufferedReader(isr)

            var line: String? = input.readLine()

            while (!line.isNullOrEmpty()) {
                ret.append(line)
                line = input.readLine()
            }
        } catch (e: Exception) {
            Log.d("GDFirebase", "FileRead Error ${e.message}/n"+Log.getStackTraceString(e))
        } finally {
            try {
                fIn?.close()
                isr?.close()
                input?.close()
            } catch (e2: Exception) {
                Log.d("GDFirebase", "${e2.message}\n" + Log.getStackTraceString(e2))
            }
        }

        return ret.toString()
    }
}
