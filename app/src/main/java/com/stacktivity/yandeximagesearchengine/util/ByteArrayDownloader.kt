package com.stacktivity.yandeximagesearchengine.util

import android.util.Log
import com.stacktivity.yandeximagesearchengine.BuildConfig.DEBUG
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.Response
import okhttp3.Callback
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.security.cert.CertPathValidatorException
import java.util.concurrent.TimeUnit

/**
 * Used for downloading files from the Internet in conjunction with [NetworkStateReceiver]
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
    private val onResult: suspend (byteBuffer: ByteBuffer?) -> Unit,
) : NetworkStateReceiver.NetworkListener() {
    private var job: Job? = null

    companion object {
        val tag: String = ByteArrayDownloader::class.java.simpleName

        private val client = OkHttpClient.Builder().apply {
            if (DEBUG) {
                eventListener(object : EventListener() {
                    override fun callStart(call: Call) {
                        val url: String = call.request().url.toString()
                        Log.d(tag, "Load url: $url")
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
        job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val buffer = try { download(/*url, timeoutMs*/) } catch (e: Exception) { null }
            if (buffer != null && buffer.remaining() > 0) {
                Log.d(tag, "download complete: $url")
            } else {
                Log.d(tag, "download failed: $url")
            }
            onResult(buffer)
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
    private suspend fun download(): ByteBuffer? = suspendCancellableCoroutine { cont ->
        val call = getOkHttpClientWithTimeout(timeoutMs)
            .newCall(
                Request.Builder()
                    .url(url)
                    .get()
                    .build()
            )

        cont.invokeOnCancellation {
            call.cancel()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                var res: ByteBuffer? = null
                response.body?.let { body ->
                    val contentLength = body.contentLength().toInt()
                    if (contentLength > 0) {
                        body.byteStream().use {
                            val buffer = ByteBuffer.allocateDirect(contentLength)
                            Channels.newChannel(it).use { channel ->
                                while (buffer.remaining() > 0) {
                                    channel.read(buffer)
                                }
                            }
                            buffer.rewind()
                            res = buffer
                        }
                    }
                }
                cont.resumeWith(Result.success(res))
            }
        })
    }
}