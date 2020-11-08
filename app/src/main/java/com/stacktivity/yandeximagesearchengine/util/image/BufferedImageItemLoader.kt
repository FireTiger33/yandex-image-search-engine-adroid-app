package com.stacktivity.yandeximagesearchengine.util.image

import android.graphics.Bitmap
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils.Companion.getGifSize
import com.stacktivity.yandeximagesearchengine.util.Constants
import kotlinx.coroutines.*
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.lang.NullPointerException
import java.nio.ByteBuffer

class BufferedImageItemLoader(
    private val cacheDir: String,
    var priorityMaxImageWidth: Int? = null
) : BufferedImageProvider<ImageItem> {

    private class MapperImageObserver {
        companion object {
            fun mapFrom(observer: ImageObserver): ImageDownloadHelper.ImageObserver {
                return object : ImageDownloadHelper.ImageObserver() {
                    override fun onBitmapResult(bitmap: Bitmap?) {
                        if (bitmap != null) {
                            observer.onBitmapResult(bitmap)
                        } else {
                            observer.onException(NullPointerException())
                        }
                    }

                    override fun onGifResult(buffer: ByteBuffer) {
                        val gifSize = getGifSize(buffer)
                        observer.onGifResult(GifDrawable(buffer), gifSize.first, gifSize.second)
                    }

                }
            }
        }
    }

    override fun getImage(
        item: ImageItem,
        imageObserver: ImageObserver,
        previewImageObserver: ImageObserver?,
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
        val fileName = item.hashCode()
        return File(cacheDir + File.separator + fileName)
    }

    private fun downloadImage(
        imageUrls: List<String>,
        imageObserver: ImageDownloadHelper.ImageObserver,
    ) {
        ImageDownloadHelper.getInstance().getOneOfImage(
            poolTag = tag,
            urls = imageUrls,
            reqWidth = priorityMaxImageWidth,
            minWidth = Constants.MIN_IMAGE_WIDTH, minHeight = Constants.MIN_IMAGE_HEIGHT,
            timeoutMs = 3000,
            imageObserver = imageObserver
        )
    }

    private suspend fun resultFromCache(
        cacheFile: File,
        imageObserver: ImageObserver
    ): Boolean {
        return BufferedImageLoader.resultFromCache(cacheFile, imageObserver)
    }

    private fun getCachingObserver(
        imageObserver: ImageObserver,
        cacheFile: File
    ): ImageDownloadHelper.ImageObserver {
        return BufferedImageLoader.getCachingObserver(imageObserver, cacheFile)
    }

    private fun downloadPreview(url: String, imageObserver: ImageDownloadHelper.ImageObserver) {
        ImageDownloadHelper.getInstance().getImageAsync(
            poolTag = tag + "_thumb",
            url = url,
            timeoutMs = 2000,
            imageObserver = imageObserver
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