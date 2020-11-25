package com.stacktivity.yandeximagesearchengine.util.image

import java.nio.ByteBuffer
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.srcRect
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.CacheWorker
import com.stacktivity.yandeximagesearchengine.util.Downloader
import com.stacktivity.yandeximagesearchengine.util.Downloader.Observer

/**
 * Image loader implementation of the [BufferedImageProvider] interface.
 * Used for downloading image and getting result in [ImageObserver]
 *
 * When image is uploaded again, result will be returned from device cache
 *
 * @see Downloader for more details about the download procedure
 */
class BufferedImageLoader : BufferedImageProvider<String> {

    internal class CachingObserver(
        private val imageObserver: Observer,
        private val cacheFile: File
    ): Observer {
        override fun onSuccess(buffer: ByteBuffer, url: String) {
            imageObserver.onSuccess(buffer, url)
            CacheWorker.saveBytesToFile(buffer, cacheFile)
        }

        override fun onError(url: String) {
            imageObserver.onError(url)
        }
    }

    override fun getImage(
        item: String,
        imageObserver: ImageObserver,
        previewImageObserver: BitmapObserver?,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val cacheFile = getCacheFile(item)
            if (resultFromCache(cacheFile, imageObserver)) {
                return@launch
            }
            val observer = ImageFactory(imageObserver)
            val cachingObserver = CachingObserver(observer, cacheFile)

            Downloader.downloadAsync(tag, item, cachingObserver)
        }
    }

    override fun getCacheFile(item: String): File {
        val fileName = item.hashCode().toString()
        return CacheWorker.getFile(fileName)
    }

    companion object {
        private val tag: String = BufferedImageLoader::class.java.simpleName

        /**
         * Used for loading image from cache and getting result in [ImageObserver]
         *
         * @return false if file does not exist or file is not image
         *         and true if result will be provided to observer
         */
        internal suspend fun resultFromCache(
            cacheFile: File,
            imageObserver: ImageObserver
        ): Boolean {
            var res = false
            val fileExists = withContext(Dispatchers.IO) { cacheFile.exists() }
            if (!fileExists) {
                return false
            }

            if (BitmapUtils.fileIsAnGifImage(cacheFile.path)) {
                loadCachedGif(cacheFile, imageObserver)
                res = true
            } else if (BitmapUtils.fileIsAnImage(cacheFile.path)) {
                res = true
                loadCachedBitmap(cacheFile, imageObserver)
            }

            return res
        }

        private suspend fun loadCachedGif(file: File, imageObserver: ImageObserver) {
            val drawable = GifDrawable(file)
            val size = drawable.srcRect
            withContext(Dispatchers.Main) {
                imageObserver.onGifResult(drawable, size.width(), size.height())
            }
        }

        private suspend fun loadCachedBitmap(file: File, imageObserver: ImageObserver) {
            BitmapUtils.getBitmapFromFile(file) { bitmap ->
                if (bitmap != null) withContext(Dispatchers.Main) {
                    imageObserver.onBitmapResult(bitmap)
                }
            }
        }
    }
}