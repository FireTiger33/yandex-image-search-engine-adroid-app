package com.stacktivity.yandeximagesearchengine.data

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.io.File


object NetworkRepository {
    val tag: String = NetworkRepository::class.java.simpleName

    fun getFileUrl(filePath: String, onResult: (url: String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageFile = File(filePath)
        val fileUri = Uri.fromFile(imageFile)
        storageRef.child("images/${imageFile.name}").run {
            putFile(fileUri)
                .continueWithTask { this.downloadUrl }
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val downloadUri = it.result.toString()
                        Log.d(tag, "image url: $downloadUri")
                        onResult(downloadUri)
                    } else it.exception?.printStackTrace()
                }
        }
    }
}