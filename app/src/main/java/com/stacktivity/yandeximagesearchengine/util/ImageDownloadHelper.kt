package com.stacktivity.yandeximagesearchengine.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.NullPointerException
import java.net.URL

class ImageDownloadHelper {

    companion object {
        /**
         * Possible to catch exceptions:
         * 1) SocketTimeoutException
         * 2) OutOfMemoryError
         * 3) CertPathValidatorException
         *
         * @param url       the direct link to image
         * @param timeoutMs max time to wait for connection to image server in milliseconds
         *
         * @return image Bitmap or null if download failed
         */
        suspend fun getBitmapAsync(url: String,
                                   reqWidth: Int? = null, reqHeight: Int? = null,
                                   timeoutMs: Int? = null
        ): Bitmap? =
            withContext(Dispatchers.IO) {
                var bitmap: Bitmap? = null

                try {
                    val conn = URL(url).openConnection().apply {
                        if (timeoutMs != null) {
                            connectTimeout = timeoutMs
                            readTimeout = timeoutMs
                        }
                    }
                    bitmap = if (reqWidth != null && reqHeight != null) {
                        try {
                            Bitmap.createScaledBitmap(BitmapFactory.decodeStream(conn.getInputStream()), reqWidth, reqHeight, false)
                        } catch (e: NullPointerException) {
                            null
                        }
                    } else {
                        BitmapFactory.decodeStream(conn.getInputStream())
                    }
                } catch (e: OutOfMemoryError) {}
                catch (e: IOException) {}

                return@withContext bitmap
            }
    }
}

