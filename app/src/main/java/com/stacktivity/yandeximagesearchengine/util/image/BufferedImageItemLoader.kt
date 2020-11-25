package com.stacktivity.yandeximagesearchengine.util.image

import android.graphics.Bitmap
import android.util.Log
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.util.ConcatIterator
import com.stacktivity.yandeximagesearchengine.util.Downloader
import com.stacktivity.yandeximagesearchengine.util.image.BufferedImageLoader.CachingObserver
import kotlinx.coroutines.*
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.lang.Runnable

/**
 * [ImageItem] loader implementation of the [BufferedImageProvider] interface.
 * Used to load the most optimal image and get the result in [ImageObserver].
 * Priority is given to images with the highest resolution.
 *
 * When image is uploaded again, result will be returned from device cache
 *
 * @param priorityMaxImageWidth determines size of most appropriate image
 *
 * @see Downloader for more details about the download procedure
 */
class BufferedImageItemLoader(
    private val cacheDir: String,
    var priorityMaxImageWidth: Int? = null
) : BufferedImageProvider<ImageItem> {

    override fun getImage(
        item: ImageItem,
        imageObserver: ImageObserver,
        previewImageObserver: BitmapObserver?,
    ) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val cacheFile = getCacheFile(item)
            if (resultFromCache(cacheFile, imageObserver)) {
                return@launch
            }
            val imageNum = getMaxAllowSizePreviewNum(item.dups)
            val cachingObserver = getCachingObserver(imageObserver, cacheFile)
            val imageUrls = (
                    item.dups.slice(0..imageNum).reversed() +
                            item.dups.slice(imageNum + 1 until item.dups.size)
                    ).map { x -> x.url }

            previewImageObserver?.let { downloadPreview(item.thumb.url, MapperImageObserver.mapFrom(it)) }
            downloadImage(imageUrls, cachingObserver)
        }
    }

    override fun getCacheFile(item: ImageItem): File {
        val fileName = item.thumb.hashCode()
        return File(cacheDir + File.separator + fileName)
    }

    private fun downloadImage(
        imageData: Iterator<ImageData>,
        imageObserver: ImageObserver,
        cacheFile: File
    ) {

        val listener = object : Runnable {
            private var i = -1

            private val localObserver = object : ImageObserver() {
                override fun onGifResult(drawable: GifDrawable, width: Int, height: Int) {
                    imageObserver.onGifResult(drawable, width, height)
                }

                override fun onBitmapResult(bitmap: Bitmap) {
                    imageObserver.onBitmapResult(bitmap)
                }

                override fun onException(e: Throwable) {
                    Log.e(tag, "${e.message}")
                    run()
                }

            }

            override fun run() {
                if (imageData.hasNext()) {
                    imageData.next().let {
                        val observer = CachingObserver(ImageFactoryWithSizeValidation(
                            localObserver,
                            it.width, it.height
                        ),
                            cacheFile
                        )
                        Downloader.downloadAsync(tag, it.url, observer/*, 3000*/)
                    }
                } else {
                    imageObserver.onException(IndexOutOfBoundsException())
                }
            }
        }

        listener.run()
    }

    private suspend fun resultFromCache(
        cacheFile: File,
        imageObserver: ImageObserver
    ): Boolean {
        return BufferedImageLoader.resultFromCache(cacheFile, imageObserver)
    }

    private fun downloadPreview(url: String, imageObserver: BitmapObserver) {
        Downloader.downloadAsync(
            poolTag = tag + "_thumb",
            url = url,
            observer = BitmapFactory(imageObserver),
            timeoutMs = 2000,
        )
    }

    private fun getMaxAllowSizePreviewNum(images: List<ImageData>): Int {
        val reqWidth = priorityMaxImageWidth ?: Int.MAX_VALUE
        images.forEachIndexed { i, preview ->
            if (preview.width <= reqWidth) {
                return i /*if (preview.width >= minImageWidth || i == 0) i else i - 1*/
            }
        }

        return images.size - 1  // returns index of image with min size
    }

    companion object {
        private val tag: String = BufferedImageItemLoader::class.java.simpleName
    }
}