package com.stacktivity.yandeximagesearchengine.util.image

import com.stacktivity.yandeximagesearchengine.util.getImage
import com.stacktivity.yandeximagesearchengine.util.getBitmap
import com.stacktivity.yandeximagesearchengine.util.Downloader.Observer
import com.stacktivity.yandeximagesearchengine.util.CacheWorker
import com.stacktivity.yandeximagesearchengine.util.CachingDownloaderObserver
import com.stacktivity.yandeximagesearchengine.util.NetworkStateReceiver.NetworkListener
import java.io.File

abstract class AbstractBufferedSmartBitmapDownloader(
    taskId: Any,
    poolTag: String,
    observer: BitmapObserver,
) : AbstractSmartBitmapDownloader(taskId, poolTag, observer) {

    override fun downloadAsync(url: String, imageFactory: Observer): NetworkListener {
        val cachingImageFactory = CachingDownloaderObserver(imageFactory, getCacheFileName(url))
        return super.downloadAsync(url, cachingImageFactory)
    }

    fun getCacheFile(url: String) = getCacheFileByUrl(url)
}

class BufferedSmartBitmapDownloader(
    val url: String, poolTag: String, observer: BitmapObserver
) : AbstractBufferedSmartBitmapDownloader(url, poolTag, observer) {

    override fun onStartDownload(): NetworkListener {
        return downloadAsync(url, BitmapFactory(this))
    }

    override fun download() {
        val cacheFile = getCacheFile(url)
        if (cacheFile.exists() && cacheFile.getBitmap(observer)) {
            // image loaded successfully from cache
        } else {
            super.download()
        }
    }

    companion object {
        fun removeAllTasksByPoolTag(poolTag: String) {
            SmartBitmapDownloader.removeAllTasksByPoolTag(poolTag)
        }

        fun getCacheFile(url: String): File {
            return getCacheFileByUrl(url)
        }
    }
}

abstract class AbstractBufferedSmartImageDownloader(
    taskId: Any,
    poolTag: String,
    observer: ImageObserver,
) : AbstractSmartImageDownloader(taskId, poolTag, observer) {
    override fun downloadAsync(url: String, imageFactory: Observer): NetworkListener {
        val cachingImageFactory = CachingDownloaderObserver(imageFactory, getCacheFileName(url))
        return super.downloadAsync(url, cachingImageFactory)
    }

    fun getCacheFile(url: String) = getCacheFileByUrl(url)
}

class BufferedSmartImageDownloader(
    val url: String,
    poolTag: String,
    observer: ImageObserver,
) : AbstractBufferedSmartImageDownloader(url, poolTag, observer) {
    override fun onStartDownload(): NetworkListener {
        return downloadAsync(url, ImageFactory(this))
    }

    override fun download() {
        val cacheFile = getCacheFile(url)
        if (cacheFile.exists() && cacheFile.getImage(observer as ImageObserver)) {
            // image loaded successfully from cache
        } else {
            super.download()
        }
    }

    companion object {
        fun removeAllTasksByPoolTag(poolTag: String) {
            SmartBitmapDownloader.removeAllTasksByPoolTag(poolTag)
        }

        fun getCacheFile(url: String): File {
            return getCacheFileByUrl(url)
        }
    }
}

private fun getCacheFileName(url: String): String {
    return url.hashCode().toString()
}

private fun getCacheFileByUrl(url: String): File {
    return CacheWorker.getFile(getCacheFileName(url))
}