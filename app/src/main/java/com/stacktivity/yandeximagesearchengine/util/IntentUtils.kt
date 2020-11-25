package com.stacktivity.yandeximagesearchengine.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.stacktivity.yandeximagesearchengine.BuildConfig
import java.io.File

fun sendImage(imageFile: File, context: Context) {
    val imageUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, imageFile)
    val imageShareIntent = Intent(Intent.ACTION_SEND).apply {
        setDataAndTypeAndNormalize(imageUri, "image/gif")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    ContextCompat.startActivity(context, imageShareIntent, null)
}

fun showImage(imageFile: File, context: Context) {
    val imageUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, imageFile)
    val imageShareIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndTypeAndNormalize(imageUri, "image/gif")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    ContextCompat.startActivity(context, imageShareIntent, null)
}