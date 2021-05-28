package com.stacktivity.yandeximagesearchengine.util

import com.stacktivity.yandeximagesearchengine.util.image.BitmapObserver
import com.stacktivity.yandeximagesearchengine.util.image.ImageObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.srcRect
import java.io.File


/**
 * Observer that saves the loaded data to a file after using it
 *
 * @see Downloader
 */
class CachingDownloaderObserver(
    private val observer: Downloader.Observer,
    private val cacheFileName: String
) : Downloader.Observer {
    override fun onSuccess(buffer: ByteArray, url: String) {
        observer.onSuccess(buffer, url)
        CacheWorker.saveBytesToFile(buffer, CacheWorker.getFile(cacheFileName))
    }

    override fun onError(url: String, e: Throwable?) {
        observer.onError(url, e)
    }
}

/**
 * Used for loading image from cache and getting a positive result in [ImageObserver]
 *
 * @return false if file does not exist or file is not image
 *         and true if result will be provided to observer
 */
fun File.getImage(imageObserver: ImageObserver): Boolean {
    return if (BitmapUtils.fileIsAnGifImage(path)) {
        loadCachedGif(this, imageObserver)
        true
    } else getBitmap(imageObserver)
}

/**
 * Used for loading image bitmap from cache and getting a positive result in [BitmapObserver]
 *
 * @return false if file does not exist or file is not image
 *         and true if result will be provided to observer
 */
fun File.getBitmap(bitmapObserver: BitmapObserver): Boolean {
    return if (BitmapUtils.fileIsAnImage(path)) {
        BitmapUtils.getBitmapFromFile(this) { bitmap ->
            if (bitmap != null) {
                bitmapObserver.onBitmapResult(bitmap)
            }
        }
        true
    } else false
}

private fun loadCachedGif(file: File, imageObserver: ImageObserver) {
    CoroutineScope(Dispatchers.Main.immediate).launch(Dispatchers.IO) {
        val drawable = GifDrawable(file)
        val size = drawable.srcRect
        withContext(Dispatchers.Main.immediate) {
            imageObserver.onGifResult(drawable, size.width(), size.height())
        }
    }
}