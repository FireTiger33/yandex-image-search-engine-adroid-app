package com.stacktivity.yandeximagesearchengine.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stacktivity.yandeximagesearchengine.BuildConfig.DEBUG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImageDownloadHelper private constructor() {

    companion object {
        val tag = ImageDownloadHelper::class.java.simpleName
        var maxQueueCount: Int = 2  // number of images loading simultaneously

        private var INSTANCE: ImageDownloadHelper? = null
        fun getInstance() = INSTANCE
            ?: ImageDownloadHelper().also {
                INSTANCE = it
            }
    }

    private class ImageDownloader(
        private val url: String,
        private val reqWidth: Int? = null, private val reqHeight: Int? = null,
        private val timeoutMs: Long? = null,
        private val onResult: (bitmap: Bitmap?) -> Unit
    ) : NetworkStateReceiver.NetworkListener() {
        private var job: Job? = null

        companion object {
            private val client = OkHttpClient.Builder().apply {
                if (DEBUG) {
                    eventListener(object : EventListener() {
                        override fun callStart(call: Call) {
                            super.callStart(call)
                            Log.d(tag, "Load url: ${call.request().url()}")
                        }

                        override fun callFailed(call: Call, ioe: IOException) {
                            // TODO if CertPathValidatorException use proxy
                            Log.e(tag, "connectionFailed: ${ioe.message}, url = ${call.request().url()}")
                        }
                    })
                }
            }.build()
        }

        override fun onNetworkIsConnected(onSuccess: () -> Unit) {
            job = downloadBitmap(url, reqWidth, reqHeight, timeoutMs) { bitmap ->
                onSuccess()
                onResult(bitmap)
            }
        }

        override fun onNetworkIsDisconnected() {
            job?.cancel()
        }

        override fun onCancel() {
            job?.cancel()
        }

        /**
         * Asynchronous image loading using [OkHttpClient]
         *
         * @param url       - direct link to image
         * @param reqWidth  - required max width to size reduce, can be omitted
         * @param reqHeight - required max height to size reduce, can be omitted
         * @param timeoutMs - connect timeout, can be omitted
         * @param onResult  - function without a return value,
         *                    which will be passed the result of the load.
         *                    Called in the main thread
         *
         * @return [Job] for task managing
         */
        private fun downloadBitmap(
            url: String,
            reqWidth: Int? = null, reqHeight: Int? = null,
            timeoutMs: Long? = null,
            onResult: (bitmap: Bitmap?) -> Unit
        ): Job = GlobalScope.launch(Dispatchers.IO) {
            var bitmap: Bitmap? = null

            /** Possible to catch exceptions:
             * 1) SocketTimeoutException
             * 2) OutOfMemoryError
             * 3) CertPathValidatorException */
            try {
                val stream = if (timeoutMs != null) {
                    client.newBuilder()
                        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                        .readTimeout(1000, TimeUnit.MILLISECONDS)
                        .build()
                } else {
                    client
                }
                    .newCall(
                        Request.Builder()
                            .url(url)
                            .get()
                            .build()
                    )
                    .execute().body()?.byteStream()

                bitmap = if (reqWidth != null || reqHeight != null) {
                    try {
                        with(BitmapFactory.decodeStream(stream)) {
                            val mReqWidth: Int = reqWidth
                                ?: (reqHeight!!.toFloat() / this.height * this.width).toInt()
                            val mReqHeight: Int = reqHeight
                                ?: (reqWidth!!.toFloat() / this.width * this.height).toInt()

                            if (mReqWidth < this.width) {
                                this
                            } else {
                                Bitmap.createScaledBitmap(this, mReqWidth, mReqHeight, false)
                            }
                        }
                    } catch (e: RuntimeException) {
                        null
                    }
                } else {
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    shortToast("OutOfMemory")
                }
            } catch (e: IOException) {
            }

            withContext(Dispatchers.Main) {
                onResult(bitmap)
            }
        }
    }

    fun getOneOfBitmap(
        poolTag: String,
        urls: Array<String>,
        reqWidth: Int? = null, reqHeight: Int? = null,
        timeoutMs: Long? = null
    ): LiveData<Bitmap?> {
        val bitmapLiveData = MutableLiveData<Bitmap?>()
        val listener = object : Runnable {
            private var i = 0

            override fun run() {
                if (i < urls.size) {
                    getBitmapAsync(poolTag, urls[i], reqWidth, reqHeight, timeoutMs) { bitmap ->
                        if (bitmap != null) bitmapLiveData.value = bitmap
                        else run()
                    }
                    i++
                } else {
                    bitmapLiveData.value = null
                }
            }

        }

        listener.run()

        return bitmapLiveData
    }

    /**
     * Download image
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
    fun getBitmapAsync(
        poolTag: String,
        url: String,
        reqWidth: Int? = null, reqHeight: Int? = null,
        timeoutMs: Long? = null,
        onResult: (bitmap: Bitmap?) -> Unit
    ) {
        if (NetworkStateReceiver.getInstance().getListenersCountByTag(poolTag) == null) {
            NetworkStateReceiver.getInstance().addNewPoolListeners(poolTag, maxQueueCount)
        }

        NetworkStateReceiver.getInstance().addListener(
            poolTag,
            ImageDownloader(url, reqWidth, reqHeight, timeoutMs, onResult)
        )
    }

    fun getBitmapAsync(
        poolTag: String,
        url: String,
        reqWidth: Int? = null, reqHeight: Int? = null,
        timeoutMs: Long? = null
    ): LiveData<Bitmap?> {
        val bitmapLiveData = MutableLiveData<Bitmap>()

        if (NetworkStateReceiver.getInstance().getListenersCountByTag(poolTag) == null) {
            NetworkStateReceiver.getInstance().addNewPoolListeners(poolTag, maxQueueCount)
        }

        NetworkStateReceiver.getInstance().addListener(
            poolTag,
            ImageDownloader(url, reqWidth, reqHeight, timeoutMs) { bitmap ->
                bitmapLiveData.value = bitmap
            }
        )

        return bitmapLiveData
    }
}
