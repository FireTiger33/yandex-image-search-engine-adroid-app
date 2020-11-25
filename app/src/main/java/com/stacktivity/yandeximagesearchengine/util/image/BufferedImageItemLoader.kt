package com.stacktivity.yandeximagesearchengine.util.image

import android.graphics.Bitmap
import android.util.Log
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.util.CacheWorker
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

            val sortedIterator = getSortedIterator(item.dups)

            previewImageObserver?.let { downloadPreview(item.thumb.url, previewImageObserver) }
            downloadImage(sortedIterator, imageObserver, getCacheFile(item))
        }
    }

    override fun getCacheFile(item: ImageItem): File {
        val fileName = item.thumb.hashCode().toString()
        return CacheWorker.getFile(fileName)
    }

    private fun downloadImage(
        imageData: Iterator<ImageData>,
        imageObserver: ImageObserver,
        cacheFile: File
    ) {

        val listener = object : Runnable {
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
                return i
            }
        }

        return images.lastIndex  // returns index of image with min size
    }

    /**
     * Creates an iterator for traversing data in the optimal order
     *
     * The first two are the most appropriate [priorityMaxImageWidth],
     * then in ascending order of size.
     * At the end there are elements descending from priority size.
     */
    private fun getSortedIterator(data: List<ImageData>): Iterator<ImageData> {
        val imageNum = getMaxAllowSizePreviewNum(data)
        var sortedIterator: Iterator<ImageData> = if (imageNum < data.lastIndex) {
            val nextImageIsAllow = if (priorityMaxImageWidth != null) {
                data[imageNum + 1].width >= 0.5 * priorityMaxImageWidth!!
            } else false
            val nextImageIndex = if (nextImageIsAllow) imageNum + 2 else imageNum + 1

            ConcatIterator(data.subList(imageNum, nextImageIndex).iterator()) +
                data.subList(0, imageNum).asReversed().iterator()
        } else {
            data.subList(0, imageNum + 1).asReversed().iterator()
        }
        if (imageNum + 1 < data.lastIndex) {
            val secondIterator = data.subList(imageNum + 2, data.size).iterator()
            sortedIterator = ConcatIterator(sortedIterator).plus(secondIterator)
        }

        return sortedIterator
    }

    companion object {
        private val tag: String = BufferedImageItemLoader::class.java.simpleName
    }
}