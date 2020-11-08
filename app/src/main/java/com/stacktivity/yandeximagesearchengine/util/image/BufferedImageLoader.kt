package com.stacktivity.yandeximagesearchengine.util.image

import java.nio.ByteBuffer
import android.graphics.Bitmap
import android.util.Log
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import kotlinx.coroutines.*
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.FileWorker.Companion.saveBytesToFile
import java.lang.NullPointerException

open class BufferedImageLoader(private val cacheDir: String) : BufferedImageProvider<String> {

    override fun getImage(
        item: String,
        imageObserver: ImageObserver,
        previewImageObserver: ImageObserver?,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val cacheFile = getCacheFile(item)
            if (resultFromCache(cacheFile, imageObserver)) {
                return@launch
            }

            val cachingObserver = getCachingObserver(imageObserver, cacheFile)

            downloadImage(item, cachingObserver)
        }
    }

    override fun getCacheFile(item: String): File {
        val fileName = item.hashCode()
        return File(cacheDir + File.separator + fileName)
    }

    companion object {
        private val tag: String = BufferedImageLoader::class.java.simpleName

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
            val drawable = withContext(Dispatchers.IO) { GifDrawable(file) }
            val imageSize = withContext(Dispatchers.IO) { BitmapUtils.getImageSize(file) }
            withContext(Dispatchers.Main) {
                imageObserver.onGifResult(drawable, imageSize.first, imageSize.second)
            }
        }

        private suspend fun loadCachedBitmap(file: File, imageObserver: ImageObserver) {
            BitmapUtils.getBitmapFromFile(file) { bitmap ->
                if (bitmap != null) withContext(Dispatchers.Main) {
                    imageObserver.onBitmapResult(bitmap)
                }
            }
        }

        internal fun getCachingObserver(
            imageObserver: ImageObserver,
            cacheFile: File?
        ): ImageDownloadHelper.ImageObserver {
            return object : ImageDownloadHelper.ImageObserver() {

                override fun onBitmapResult(bitmap: Bitmap?) {
                    if (bitmap != null) {
                        imageObserver.onBitmapResult(bitmap)
                        if (cacheFile != null) {
                            BitmapUtils.saveBitmapToFile(bitmap, cacheFile, onException = {
                                // TODO delete cache
                            })
                        }
                    } else {
                        imageObserver.onException(NullPointerException())
                    }
                }

                override fun onGifResult(buffer: ByteBuffer) {
                    val gifSize = BitmapUtils.getGifSize(buffer)
                    imageObserver.onGifResult(GifDrawable(buffer), gifSize.first, gifSize.second)
                    if (cacheFile != null) {
                        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                            saveBytesToFile(buffer, cacheFile)
                            Log.d(tag, "Gif save complete: ${cacheFile.name}")
                        }
                    }
                }
            }
        }

        private fun downloadImage(
            imageUrl: String,
            imageObserver: ImageDownloadHelper.ImageObserver
        ) {
            ImageDownloadHelper.getInstance().getImageAsync(
                poolTag = tag,
                url = imageUrl,
                timeoutMs = 3000,
                imageObserver = imageObserver
            )
        }
    }
}