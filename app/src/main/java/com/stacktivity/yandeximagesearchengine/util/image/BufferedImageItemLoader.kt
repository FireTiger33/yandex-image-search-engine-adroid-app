package com.stacktivity.yandeximagesearchengine.util.image

import android.graphics.Bitmap
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.data.LoadState
import com.stacktivity.yandeximagesearchengine.util.ConcatIterator
import com.stacktivity.yandeximagesearchengine.util.NetworkStateReceiver.NetworkListener
import com.stacktivity.yandeximagesearchengine.util.getImage
import pl.droidsonroids.gif.GifDrawable

/**
 * [ImageItem] loader implementation of the [BufferedImageProvider] interface.
 * Used to load the most optimal image and get the result in [ImageObserver].
 * Priority is given to images with the highest resolution.
 *
 * When image is uploaded again, result will be returned from device cache
 *
 * @param priorityMaxImageWidth determines size of most appropriate image
 */
class BufferedImageItemLoader(
    var priorityMaxImageWidth: Int? = null
) : ImageProvider<ImageItem> {

    private class ImageItemDownloader(
        itemNum: Int,
        val iterator: Iterator<ImageData>,
        observer: ImageObserver,
    ) : AbstractBufferedSmartImageDownloader(itemNum, tag, observer) {
        private lateinit var currentDataLoader: ImageDataDownloader

        override fun onBitmapResult(bitmap: Bitmap) =
            currentDataLoader.onBitmapResult(bitmap)

        override fun onGifResult(drawable: GifDrawable, width: Int, height: Int) =
            currentDataLoader.onGifResult(drawable, width, height)

        override fun onException(e: Throwable) {
            currentDataLoader.applyExceptionStatus(e)

            if (iterator.hasNext()) {
                task = downloadNext()
            } else {
                super.onException(e)
            }
        }

        override fun onStartDownload(): NetworkListener = downloadNext()

        fun downloadNext() = iterator.next().let {
            currentDataLoader = ImageDataDownloader(it, tag, imageObserver)
            return@let downloadAsync(
                imageFactory = ImageFactoryWithSizeValidation(this, it.width, it.height),
                url = it.url
            )
        }

        companion object {
            fun removeAllTasksByPoolTag(poolTag: String) {
                SmartImageDownloader.removeAllTasksByPoolTag(poolTag)
            }
        }
    }

    override fun getImage(
        item: ImageItem,
        imageObserver: ImageObserver,
        previewImageObserver: BitmapObserver?,
    ) {
        val sortedIterator: Iterator<ImageData> by lazy { getSortedIterator(item.dups) }
        val loadedImages = item.dups.filter { it.loadState == LoadState.Loaded }

        if (loadedImages.isNotEmpty()) {
            val loadedImage = getSortedIterator(loadedImages).next()
            val cacheFile = BufferedSmartImageDownloader.getCacheFile(loadedImage.url)
            if (cacheFile.getImage(imageObserver)) {
                return
            }
        }

        previewImageObserver?.let {
            val previewUrl = item.thumb.url
            downloadPreview(previewUrl, it)
        }

        ImageItemDownloader(item.itemNum, sortedIterator, imageObserver)
            .download()
    }

    fun removeAllTasks() {
        ImageItemDownloader.removeAllTasksByPoolTag(tag)
        SmartBitmapDownloader.removeAllTasksByPoolTag(tagThumb)
    }

    private fun downloadPreview(url: String, imageObserver: BitmapObserver) {
        BufferedSmartBitmapDownloader(
            poolTag = tagThumb,
            url = url,
            observer = imageObserver,
        ).download()
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
            val secondIterator = data.subList(imageNum + 2, data.lastIndex).iterator()
            sortedIterator = ConcatIterator(sortedIterator).plus(secondIterator)
        }

        return sortedIterator
    }

    companion object {
        private val tag: String = BufferedImageItemLoader::class.java.simpleName
        private val tagThumb = "${tag}_thumb"
    }
}