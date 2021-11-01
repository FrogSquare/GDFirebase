package com.frogsquare.firebasestorage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast

import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.frogsquare.firebaseauth.GDFirebaseAuth

import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot

import java.io.File

const val TAG: String = "GDFirebaseStorage"

@Suppress("UNUSED")
class GDFirebaseStorage constructor(godot: Godot): GodotPlugin(godot) {

    private val context: Context = godot.requireContext()
    private val mBroadcastReceiver: BroadcastReceiver

    init {
        this.mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {

                when (intent.action) {
                    DownloadService.DOWNLOAD_COMPLETED -> {
                        Toast.makeText(
                            activity!!,
                            "Download Complete",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    DownloadService.DOWNLOAD_ERROR -> {
                        Toast.makeText(
                            activity!!,
                            "Download Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(activity!!)
            .registerReceiver(mBroadcastReceiver, DownloadService.intentFilter)
    }

    @UsedByGodot
    fun download(url: String, path: String) {
        if (!GDFirebaseAuth.isSignedIn) return

        val intent = Intent(activity!!, DownloadService::class.java)
            .putExtra(DownloadService.EXTRA_DOWNLOAD_PATH, url)
            .putExtra(DownloadService.EXTRA_DOWNLOAD_TO, path)
            .setAction(DownloadService.ACTION_DOWNLOAD)

        activity?.startService(intent)
    }

    @UsedByGodot
    fun upload(filePath: String, child: String) {
        if (!GDFirebaseAuth.isSignedIn) return

        var path = ""
        if (filePath.startsWith("user://")) {
            path = filePath.replaceFirst("user://", "")
            path = Godot.io.dataDir.toString() + "/" + path
        } else {
            path = Environment.getDataDirectory().absoluteFile.toString() + "/" + filePath
        }

        Log.d(TAG, "Uploading::$path")
        val fileUri = Uri.fromFile(File(path))

        // Kick off UploadService to upload the file
        val intent = Intent(activity, UploadService::class.java)
            .putExtra(UploadService.EXTRA_FILE_URI, fileUri)
            .putExtra(UploadService.EXTRA_FILE_CHILD, child)
            .setAction(UploadService.ACTION_UPLOAD)

        Log.d(TAG, "Starting:::UploadService")
        activity!!.startService(intent)
    }

    override fun onMainDestroy() {
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(mBroadcastReceiver)
    }

    override fun getPluginName(): String {
        return "GDFirebaseStorage"
    }
}