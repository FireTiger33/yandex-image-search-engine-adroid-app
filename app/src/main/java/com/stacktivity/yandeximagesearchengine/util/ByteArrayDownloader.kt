package com.stacktivity.yandeximagesearchengine.util

import android.util.Log
import com.stacktivity.yandeximagesearchengine.BuildConfig.DEBUG
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.concurrent.TimeUnit

/**
 * Used for downloading files from the Internet in conjunction with NetworkStateReceiver
 *
 * @param url       - direct link to image
 * @param timeoutMs - connect timeout, can be omitted
 * @param onResult  - function without a return value,
 *                    which will be passed the result of the load.
 *                    Called in the main thread
 */
class ByteArrayDownloader(
    private val url: String,
    private val timeoutMs: Long? = null,
    private val onResult: (byteBuffer: ByteBuffer?) -> Unit
) : NetworkStateReceiver.NetworkListener() {
    private var job: Job? = null

    companion object {
        val tag = ByteArrayDownloader::class.java.simpleName

        private val client = OkHttpClient.Builder().apply {
            if (DEBUG) {
                eventListener(object : EventListener() {
                    override fun callStart(call: Call) {
                        super.callStart(call)
                        Log.d(tag, "Load url: ${call.request().url()}")
                    }

                    override fun callFailed(call: Call, ioe: IOException) {
                        // TODO if CertPathValidatorException use proxy
                        if (NetworkStateReceiver.networkIsConnected(NetworkStateReceiver.getInstance())) {
                            Log.e(tag, "connectionFailed: ${ioe.message}, url = ${call.request().url()}")
                        }
                    }
                })
            }
        }.build()
    }

    override fun onNetworkIsConnected(onSuccess: () -> Unit) {
        job = download(url, timeoutMs) { buffer ->
            // TODO if gif
            if (NetworkStateReceiver.networkIsConnected(NetworkStateReceiver.getInstance())) {
                onResult(buffer)
            }
            if (buffer != null && buffer.remaining() > 0) {
                Log.d(tag, "download complete: $url")
            } else {
                Log.d(tag, "download failed: $url")
            }
            onSuccess()
        }
    }

    override fun onNetworkIsDisconnected() {
        job?.cancel()
    }

    override fun onCancel() {
        job?.cancel()
    }

    private fun getOkHttpClientWithTimeout(timeoutMs: Long?): OkHttpClient {
        return if (timeoutMs != null) {
            client.newBuilder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(1500, TimeUnit.MILLISECONDS)
                .build()
        } else {
            client
        }
    }

    /**
     * Asynchronous image loading using [OkHttpClient]
     *
     * @return [Job] for task managing
     */
    private fun download(  // todo
        url: String,
        timeoutMs: Long? = null,
        onResult: (buffer: ByteBuffer?) -> Unit
    ): Job = GlobalScope.launch(Dispatchers.IO) {
        var result: ByteBuffer? = null

        try {
            val contentLength: Long
            val stream = getOkHttpClientWithTimeout(timeoutMs)
                .newCall(
                    Request.Builder()
                        .url(url)
                        .get()
                        .build()
                )
                .await().apply {
                    contentLength = contentLength()
                }
                .byteStream()

            stream.use {
                if (contentLength < 1) return@use
                val buffer = ByteBuffer.allocateDirect(contentLength.toInt())
                Channels.newChannel(it).use { channel ->
                    while (buffer.remaining() > 0) {
                        channel.read(buffer)
                    }
                    result = buffer
                    Log.i(tag, "BufferSize: ${result?.capacity()}")
                }
            }
        } catch (e: OutOfMemoryError) {  // TODO
            e.printStackTrace()
            if (DEBUG) {
                withContext(Dispatchers.Main) {
                    shortToast("OutOfMemory")
                }
            }
        } catch (t: Throwable) {
            if (t !is CancellationException) t.printStackTrace()
            /** Possible more exceptions:
             * 1) SocketTimeoutException
             * 3) CertPathValidatorException */
        }
        withContext(Dispatchers.Main) {
            onResult(result)
        }
    }
}