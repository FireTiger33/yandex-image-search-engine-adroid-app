package com.stacktivity.yandeximagesearchengine.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.*
import java.io.IOException
import java.net.URL

class ImageDownloadHelper {

    companion object {
        val tag = ImageDownloadHelper::class.java.simpleName
        /**
         * Possible exceptions:
         * 1) SocketTimeoutException
         * 2) OutOfMemoryError
         * 3) CertPathValidatorException
         */
        suspend fun getBitmapAsync(url: String, timeoutMs: Int): Bitmap? =
            withContext(Dispatchers.IO) {
                var bitmap: Bitmap? = null

                try {
                    val conn = URL(url).openConnection().apply {
                        connectTimeout = timeoutMs
                        readTimeout = timeoutMs
                    }
                    bitmap = BitmapFactory.decodeStream(conn.getInputStream())
                } catch (e: IOException) {}

                return@withContext bitmap
            }
    }
}

