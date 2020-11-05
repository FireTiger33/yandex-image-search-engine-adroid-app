package com.stacktivity.yandeximagesearchengine.util

import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun okhttp3.Call.await(): okhttp3.ResponseBody =
    suspendCancellableCoroutine {
        this.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                it.resumeWithException(e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body()
                if (body != null && response.isSuccessful) {
                    it.resume(body)
                } else {
                    // TODO use proxy
                    it.resumeWithException(IllegalStateException("Response is not success: ${body.toString()}"))
                }
            }
        })

        it.invokeOnCancellation { this@await.cancel()}
    }
