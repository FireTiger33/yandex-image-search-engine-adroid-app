package com.stacktivity.yandeximagesearchengine.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer


/**
 * Used for uploading files as a [ByteBuffer]
 *
 * @see Downloader.downloadAsync for more details
 */
object Downloader {

    var maxQueueCount: Int = 4  // number of files loading simultaneously

    interface Observer {
        fun onSuccess(buffer: ByteBuffer, url: String)
        fun onError(url: String)
    }

    /**
     * Download file using [NetworkStateReceiver] for tracking status of network connection.
     * If connection is lost, error will not be returned,
     * and if the connection is reconnected, download will continue
     *
     * @param poolTag   files with different pool tags are loaded in parallel.
     * You can configure max number of files in one pool uploaded in parallel using the parameter
     * [Downloader.maxQueueCount]
     *
     * @param url       direct link to image
     * @param timeoutMs max time to wait for connection to file server in milliseconds
     *
     * @see NetworkStateReceiver for more details about download order
     */
    fun downloadAsync(
        poolTag: String,
        url: String,
        observer: Observer,
        timeoutMs: Long? = null
    ) {
        checkPool(poolTag)

        NetworkStateReceiver.getInstance().addListener(
            poolTag,
            ByteArrayDownloader(url, timeoutMs) { buffer ->
                withContext(Dispatchers.Main) {
                    buffer?.let {
                        it.rewind()
                        observer.onSuccess(buffer, url)
                    } ?: run {
                        observer.onError(url)
                    }
                }
            }
        )
    }

    private fun checkPool(poolTag: String) {
        if (NetworkStateReceiver.getInstance().getListenersCountByTag(poolTag) == null) {
            NetworkStateReceiver.getInstance().addNewPoolListeners(poolTag, maxQueueCount)
        }
    }
}