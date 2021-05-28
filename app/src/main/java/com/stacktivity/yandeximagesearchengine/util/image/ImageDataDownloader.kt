package com.stacktivity.yandeximagesearchengine.util.image

import android.graphics.Bitmap
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.LoadState
import com.stacktivity.yandeximagesearchengine.util.HttpException
import com.stacktivity.yandeximagesearchengine.util.NetworkStateReceiver
import com.stacktivity.yandeximagesearchengine.util.getImage
import pl.droidsonroids.gif.GifDrawable
import javax.net.ssl.SSLHandshakeException

class ImageDataDownloader(
    val data: ImageData,
    poolTag: String,
    imageObserver: ImageObserver
) : AbstractBufferedSmartImageDownloader(data.url, poolTag, imageObserver) {
    override fun onBitmapResult(bitmap: Bitmap) {
        data.loadState = LoadState.Loaded
        super.onBitmapResult(bitmap)
    }

    override fun onGifResult(drawable: GifDrawable, width: Int, height: Int) {
        data.loadState = LoadState.Loaded
        super.onGifResult(drawable, width, height)
    }

    override fun onException(e: Throwable) {
        applyExceptionStatus(e)
        super.onException(e)
    }

    fun applyExceptionStatus(e: Throwable) {
        if (e is SSLHandshakeException) {
            data.loadState = LoadState.Unreachable
        } else if (e is HttpException) {
            if (e.code == 401) data.loadState = LoadState.Unreachable
            else if (e.code == 503) data.loadState = LoadState.Protected
        } else { // SocketTimeoutException and other unexpected non-2xx HTTP response
            data.loadState = LoadState.NotAvailable
        }
    }

    override fun onStartDownload(): NetworkStateReceiver.NetworkListener {
        return downloadAsync(data.url, ImageFactory(this))
    }

    override fun download() {
        val cacheFile = getCacheFile(data.url)
        if (cacheFile.exists() && cacheFile.getImage(observer as ImageObserver)) {
            // image loaded successfully from cache
        } else {
            super.download()
        }
    }
}