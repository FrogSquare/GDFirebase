package com.frogsquare.firebasestorage


import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log

import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import com.google.firebase.storage.ktx.storage

/**
 * Service to handle uploading files to Firebase Storage.
 */
class UploadService: BaseTaskService() {

    // [START declare_ref]
    private lateinit var storageRef: StorageReference
    // [END declare_ref]

    override fun onCreate() {
        super.onCreate()
        storageRef = Firebase.storage.reference
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand:$intent:$startId")
        if (ACTION_UPLOAD == intent.action) {
            val fileUri = intent.getParcelableExtra<Uri>(EXTRA_FILE_URI)!!
            val child = intent.getStringExtra(EXTRA_FILE_CHILD)!!

            // Make sure we have permission to read the data
            contentResolver.takePersistableUriPermission(
                fileUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION)

            uploadFromUri(fileUri, child)
        }

        return START_REDELIVER_INTENT
    }

    private fun uploadFromUri(fileUri: Uri, child: String) {

    }

    // [START upload_from_uri]
    private fun uploadFromUri(fileUri: Uri, folder: String, meta: String) {
        Log.d(TAG, "uploadFromUri:src:$fileUri")

        taskStarted()
        showProgressNotification("Uploading", 0, 0)

        fileUri.lastPathSegment?.let {
            val fileRef: StorageReference = if (folder.isEmpty()) {
                storageRef.child(it)
            } else {
                storageRef.child(folder).child(it)
            }

            val uploadTask: UploadTask = if (meta.isEmpty()) {
                fileRef.putFile(fileUri)
            } else {
                val metadata = StorageMetadata.Builder()
                    .setContentType(meta)
                    .build()

                fileRef.putFile(fileUri, metadata)
            }

            uploadTask.addOnProgressListener {(bytesTransferred, totalByteCount) ->
                showProgressNotification(
                    "Uploading",
                    bytesTransferred,
                    totalByteCount)
            }.continueWithTask { task ->
                // Forward any exceptions
                if (!task.isSuccessful) {
                    throw task.exception!!
                }

                Log.d(TAG, "uploadFromUri: upload success")

                // Request the public download URL
                fileRef.downloadUrl
            }.addOnSuccessListener { downloadUri ->
                // Upload succeeded
                Log.d(TAG, "uploadFromUri: getDownloadUri success")

                // [START_EXCLUDE]
                broadcastUploadFinished(downloadUri, fileUri)
                showUploadFinishedNotification(downloadUri, fileUri)
                taskCompleted()
                // [END_EXCLUDE]
            }.addOnFailureListener { exception ->
                // Upload failed
                Log.w(TAG, "uploadFromUri:onFailure", exception)

                // [START_EXCLUDE]
                broadcastUploadFinished(null, fileUri)
                showUploadFinishedNotification(null, fileUri)
                taskCompleted()
                // [END_EXCLUDE]
            }
        }
    }
    // [END upload_from_uri]

    /**
     * Broadcast finished upload (success or failure).
     * @return true if a running receiver received the broadcast.
     */
    private fun broadcastUploadFinished(downloadUrl: Uri?, fileUri: Uri?): Boolean {
        val success = downloadUrl != null

        val action = if (success) UPLOAD_COMPLETED else UPLOAD_ERROR

        val broadcast = Intent(action)
            .putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
            .putExtra(EXTRA_FILE_URI, fileUri)
        return LocalBroadcastManager.getInstance(applicationContext)
            .sendBroadcast(broadcast)
    }

    /**
     * Show a notification for a finished upload.
     */
    private fun showUploadFinishedNotification(downloadUrl: Uri?, fileUri: Uri?) {
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
            .putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
            .putExtra(EXTRA_FILE_URI, fileUri)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val success = downloadUrl != null
        val caption = if (success) "Success" else "Failure"

        showCompletedNotification(caption, intent, success)
    }

    companion object {

        private const val TAG = "MyUploadService"

        /** Intent Actions  */
        const val ACTION_UPLOAD = "action_upload"
        const val UPLOAD_COMPLETED = "upload_completed"
        const val UPLOAD_ERROR = "upload_error"

        /** Intent Extras  */
        const val EXTRA_FILE_URI = "extra_file_uri"
        const val EXTRA_FILE_CHILD = "extra_file_child"
        const val EXTRA_DOWNLOAD_URL = "extra_download_url"

        val intentFilter: IntentFilter
            get() {
                val filter = IntentFilter()
                filter.addAction(UPLOAD_COMPLETED)
                filter.addAction(UPLOAD_ERROR)

                return filter
            }
    }
}
