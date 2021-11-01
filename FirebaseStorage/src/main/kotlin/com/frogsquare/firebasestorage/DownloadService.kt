package com.frogsquare.firebasestorage

import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.lang.Exception

class DownloadService: BaseTaskService() {

    private lateinit var storageRef: StorageReference

    override fun onCreate() {
        super.onCreate()

        // Initialize Storage
        storageRef = Firebase.storage.reference
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand:$intent:$startId")

        if (ACTION_DOWNLOAD == intent.action) {
            // Get the path to download from the intent
            val downloadPath = intent.getStringExtra(EXTRA_DOWNLOAD_PATH)!!
            val downloadTo = intent.getStringExtra(EXTRA_DOWNLOAD_TO)!!

            downloadToFile(downloadPath, downloadTo)
        }

        return START_REDELIVER_INTENT
    }

    private fun downloadToFile(downloadPath: String, downloadTo: String) {
        val rootPath = File(Environment.getDataDirectory(), downloadTo)
        if (!rootPath.exists()) {
            rootPath.mkdirs()
        }

        val fileUri = Uri.parse(downloadPath)
        val localFile = File(rootPath, fileUri.lastPathSegment!!)

        taskStarted()
        showProgressNotification("Progress downloading", 0, 0)

        storageRef.child(downloadPath).getFile(localFile).addOnProgressListener { snap ->
            onMainProgress(downloadPath, snap.bytesTransferred, snap.totalByteCount)
        }.addOnSuccessListener { (_, totalBytes) ->
            onMainSuccess(downloadPath, totalBytes)
        }.addOnFailureListener { exception ->
            onMainFailure(downloadPath, exception)
        }
    }

    private fun downloadFromPath(downloadPath: String) {
        Log.d(TAG, "downloadFromPath:$downloadPath")

        // Mark task started
        taskStarted()
        showProgressNotification("Downloading", 0, 0)

        // Download and get total bytes
        storageRef.child(downloadPath).getStream { (_, totalBytes), inputStream ->
            var bytesDownloaded: Long = 0

            val buffer = ByteArray(1024)
            var size: Int = inputStream.read(buffer)

            while (size != -1) {
                bytesDownloaded += size.toLong()
                showProgressNotification("Downloading",
                    bytesDownloaded, totalBytes)

                size = inputStream.read(buffer)
            }

            // Close the stream at the end of the Task
            inputStream.close()
        }.addOnSuccessListener { (_, totalBytes) ->
            onMainSuccess(downloadPath, totalBytes)
        }.addOnFailureListener { exception ->
            onMainFailure(downloadPath, exception)
        }
    }

    private fun onMainProgress(path: String, currentByteCount: Long, totalByteCount: Long) {
        showProgressNotification("Progress downloading", currentByteCount, totalByteCount)
    }

    private fun onMainSuccess(path: String, totalByteCount: Long) {
        Log.d(TAG, "Download::SUCCESS")

        // Send success broadcast with number of bytes downloaded
        broadcastDownloadFinished(path, totalByteCount)
        showDownloadFinishedNotification(path, totalByteCount.toInt())

        // Mark task completed
        taskCompleted()
    }

    private fun onMainFailure(path: String, exception: Exception) {
        Log.d(TAG, "Download::FAILURE::$exception")

        // Send failure broadcast
        broadcastDownloadFinished(path, -1)
        showDownloadFinishedNotification(path, -1)

        // Mark task completed
        taskCompleted()
    }

    /**
     * Broadcast finished download (success or failure).
     * @return true if a running receiver received the broadcast.
     */
    private fun broadcastDownloadFinished(downloadPath: String, bytesDownloaded: Long): Boolean {
        val success = bytesDownloaded != -1L
        val action = if (success) DOWNLOAD_COMPLETED else DOWNLOAD_ERROR

        val broadcast = Intent(action)
            .putExtra(EXTRA_DOWNLOAD_PATH, downloadPath)
            .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded)
        return LocalBroadcastManager.getInstance(applicationContext)
            .sendBroadcast(broadcast)
    }

    /**
     * Show a notification for a finished download.
     */
    private fun showDownloadFinishedNotification(downloadPath: String, bytesDownloaded: Int) {
        // Hide the progress notification
        dismissProgressNotification()

        val cls = try {
            Class.forName("com.godot.game.GodotApp")
        } catch (e: ClassNotFoundException) {
            Log.e("GDFirebase", "${e.message}\n" + Log.getStackTraceString(e))
            return
        }

        // Make Intent to MainActivity
        val intent = Intent(this, cls)
            .putExtra(EXTRA_DOWNLOAD_PATH, downloadPath)
            .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val success = bytesDownloaded != -1
        val caption = if (success) {
            "Download Success"
        } else {
            "Download Failed"
        }

        showCompletedNotification(caption, intent, true)
    }

    companion object {
        /** Actions  */
        const val ACTION_DOWNLOAD = "action_download"
        const val DOWNLOAD_COMPLETED = "download_completed"
        const val DOWNLOAD_ERROR = "download_error"

        /** Extras  */
        const val EXTRA_DOWNLOAD_PATH = "extra_download_path"
        const val EXTRA_DOWNLOAD_TO = "extra_download_to"
        const val EXTRA_BYTES_DOWNLOADED = "extra_bytes_downloaded"

        val intentFilter: IntentFilter
            get() {
                val filter = IntentFilter()
                filter.addAction(DOWNLOAD_COMPLETED)
                filter.addAction(DOWNLOAD_ERROR)

                return filter
            }
    }
}
