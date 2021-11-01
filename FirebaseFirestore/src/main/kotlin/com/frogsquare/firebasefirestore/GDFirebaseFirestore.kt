package com.frogsquare.firebasefirestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot

import org.json.JSONException
import org.json.JSONObject

private const val TAG: String = "GDFirebaseFirestore"

@Suppress("UNUSED")
class GDFirebaseFirestore constructor(godot: Godot): GodotPlugin(godot) {

    private val storage: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        FirebaseFirestore.setLoggingEnabled(true)
    }

    @UsedByGodot
    fun loadDocument(name: String) {
        storage.collection(name).get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val json = JSONObject()

                    try {
                        val child = JSONObject()
                        for (doc in it.result) {
                            child.put(doc.id, doc.data)
                        }

                        json.put(name, child)
                    } catch (e: JSONException) {
                        Log.d(TAG, "JSON Exception ${e.message}")
                    }

                    emitSignal("document_loaded", json.toString())
                } else {
                    Log.d(TAG, "Error::Getting::Documents::${it.exception}")
                }
            }
    }

    @UsedByGodot
    fun addDocument(name: String, data: Dictionary) {
        storage.collection(name).add(data)
            .addOnSuccessListener {
                emitSignal("document_added", true)
            }.addOnFailureListener {
                Log.d(TAG, "Error::Adding::Document::${it.message}")
            }
    }

    @UsedByGodot
    fun setData(colName: String, docName: String, data: Dictionary) {
        storage.collection(colName).document(docName)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                emitSignal("document_set", true)
            }.addOnFailureListener {
                Log.d(TAG, "Error::Setting::Document::${it.message}")
            }
    }

    override fun getPluginName(): String {
        return "GDFirebaseFirestore"
    }
}