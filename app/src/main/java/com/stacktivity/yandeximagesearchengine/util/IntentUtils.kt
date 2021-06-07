package com.stacktivity.yandeximagesearchengine.util

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.stacktivity.yandeximagesearchengine.BuildConfig
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils.getImageFormat
import java.io.File

class PickImage : ActivityResultContract<Any, Uri>() {
    override fun createIntent(context: Context, input: Any?): Intent =
        Intent(Intent.ACTION_PICK).apply { type = "image/*" }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (intent == null || resultCode != RESULT_OK) null
        else intent.data
}

class TakePictureToPrivateFile: ActivityResultContract<File, Boolean>() {
    override fun createIntent(context: Context, outFile: File): Intent {
        val tempFileUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, outFile)
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri)
    }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        resultCode == RESULT_OK
}

fun sendImage(imageFile: File, context: Context) {
    val imageFormat = getImageFormat(imageFile)
    // temp file with extension is used to properly send image by email or save it
    val tempFile = CacheWorker.getFileWithExtension(imageFile, imageFormat)
    val imageUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, tempFile)
    val imageShareIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (imageFormat.contains("gif")) "image/gif" else "image/*"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    ContextCompat.startActivity(context, imageShareIntent, null)
}

fun showImage(imageFile: File, context: Context) {
    val imageFormat = getImageFormat(imageFile)
    // temp file with extension is used for proper saving in another file manager
    val tempFile = CacheWorker.getFileWithExtension(imageFile, imageFormat)
    val imageUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, tempFile)
    val imageShareIntent = Intent(Intent.ACTION_VIEW).apply {
        val type = if (imageFormat.contains("gif")) "image/gif" else "image/*"
        setDataAndTypeAndNormalize(imageUri, type)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    ContextCompat.startActivity(context, imageShareIntent, null)
}