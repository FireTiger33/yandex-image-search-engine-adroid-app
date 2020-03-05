package com.stacktivity.yandeximagesearchengine.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Data
import androidx.work.Constraints
import androidx.work.NetworkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL

class ImageDownloadHelper {

    class DownloadWorker(appContext: Context, params: WorkerParameters)
        : CoroutineWorker(appContext, params) {
        companion object {
            const val KEY_IMAGE_OUTPUT_FILE_PATH = "outputFile"
            const val KEY_IMAGE_URLS = "imageUrls"
            const val KEY_REQ_WIDTH = "reqWidth"
            const val KEY_REQ_HEIGHT= "reqHeight"
            const val KEY_TIMEOUT = "timeout"
        }

        override suspend fun doWork(): Result {
            val imageUrls: Array<String> = inputData.getStringArray(KEY_IMAGE_URLS)?: return Result.failure()
            val outFilePath: String = inputData.getString(KEY_IMAGE_OUTPUT_FILE_PATH)?: return Result.failure()
            val reqWidth = inputData.getString(KEY_REQ_WIDTH)?.toInt()
            val reqHeight = inputData.getString(KEY_REQ_HEIGHT)?.toInt()
            val timeoutMs = inputData.getString(KEY_TIMEOUT)?.toInt()

            var bitmap: Bitmap?

            imageUrls.forEach { imageUrl ->
                bitmap = getBitmapAsync(imageUrl, reqWidth, reqHeight, timeoutMs)
                if (bitmap != null) {
                    Log.d(tag, "Load success: $imageUrl")

                    return if (BitmapUtils.saveBitmapToFile(bitmap!!, File(outFilePath))) {
                        Result.success()
                    } else {
                        Result.failure()
                    }
                } else {
                    Log.d(tag, "Load failed: $imageUrl")
                }
            }

            return Result.failure()
        }
    }

    companion object {
        val tag = ImageDownloadHelper::class.java.simpleName

        /**
         * Create and return [OneTimeWorkRequest] that loads image using [getBitmapAsync]
         * when an Internet connection is active
         * and save it to [outFile]
         *
         * @param url       the direct link to image
         * @param outFile   the file where image will be uploaded
         * @param timeoutMs max time to wait for connection to image server in milliseconds
         * @param reqWidth  required width
         * @param reqHeight required height
         */
        fun getWorkForLoadAndSaveImage(
            url: Array<String>,
            outFile: File,
            reqWidth: Int? = null, reqHeight: Int? = null,
            timeoutMs: Int? = null
        ): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = Data.Builder().run {
                putStringArray(DownloadWorker.KEY_IMAGE_URLS, url)
                putString(DownloadWorker.KEY_IMAGE_OUTPUT_FILE_PATH, outFile.path)
                if (reqWidth != null) putString(DownloadWorker.KEY_REQ_WIDTH, reqWidth.toString())
                if (reqHeight != null) putString(DownloadWorker.KEY_REQ_HEIGHT, reqHeight.toString())
                if (timeoutMs != null) putString(DownloadWorker.KEY_TIMEOUT, timeoutMs.toString())
                build()
            }

            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()
        }

        /**
         * Download image using URLConnection
         *
         * If only one of required resolution parameters is set, image will change proportionally
         *
         * @param url       the direct link to image
         * @param timeoutMs max time to wait for connection to image server in milliseconds
         * @param reqWidth  required width
         * @param reqHeight required height
         *
         * @return image Bitmap or null if download failed
         */
        suspend fun getBitmapAsync(url: String,
                                   reqWidth: Int? = null, reqHeight: Int? = null,
                                   timeoutMs: Int? = null
        ): Bitmap? =
            withContext(Dispatchers.IO) {
                var bitmap: Bitmap? = null

                /** Possible to catch exceptions:
                 * 1) SocketTimeoutException
                 * 2) OutOfMemoryError
                 * 3) CertPathValidatorException */
                try {
                    val conn = URL(url).openConnection().apply {
                        if (timeoutMs != null) {
                            connectTimeout = timeoutMs
                            readTimeout = timeoutMs
                        }
                    }
                    bitmap = if (reqWidth != null || reqHeight != null) {
                        try {
                            with(BitmapFactory.decodeStream(conn.getInputStream())) {
                                val mReqWidth: Int = reqWidth?: (reqHeight!!.toFloat() / this.height * this.width).toInt()
                                val mReqHeight: Int = reqHeight?: (reqWidth!!.toFloat() / this.width * this.height).toInt()
                                Bitmap.createScaledBitmap(this, mReqWidth, mReqHeight, false)
                            }
                        } catch (e: NullPointerException) {
                            null
                        }
                    } else {
                        BitmapFactory.decodeStream(conn.getInputStream())
                    }
                } catch (e: OutOfMemoryError) {
                    withContext(Dispatchers.Main) {
                        shortToast("OutOfMemory")
                    }
                }
                catch (e: IOException) {}

                return@withContext bitmap
            }
    }
}
