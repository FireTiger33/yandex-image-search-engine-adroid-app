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

class BufferedImageItemLoader: BufferedImageLoader() {
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

    companion object {
        val tag: String = BufferedImageItemLoader::class.java.simpleName

        fun getImage(
            item: ImageItem,
            reqImageWidth: Int, minImageWidth: Int,
            previewImageObserver: ImageObserver? = null,
            imageObserver: ImageObserver,
            cacheFile: File? = null,
            onImageSelected: suspend (width: Int, height: Int) -> Unit,
        ) = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {

            cacheFile?.let {
                if (resultFromCache(it, imageObserver)) {
                    return@launch
                }
            }

            val imageNum = getMaxAllowSizePreviewNum(item.dups, reqImageWidth, minImageWidth)
            val selectedImage = item.dups[imageNum]
            onImageSelected(selectedImage.width, selectedImage.height)

            val cachingObserver = getCachingObserver(imageObserver, cacheFile)

            val imageUrls = (
                    item.dups.slice(0..imageNum).reversed() +
                            item.dups.slice(imageNum + 1 until item.dups.size)
                    ).map { x -> x.url }

            previewImageObserver?.let { downloadPreview(item.thumb.url, MapperImageObserver.mapFrom(it)) }
            downloadImage(imageUrls, cachingObserver, reqImageWidth)
        }

        private fun downloadImage(
            imageUrls: List<String>,
            imageObserver: ImageDownloadHelper.ImageObserver,
            reqImageWidth: Int? = null, reqImageHeight: Int? = null
        ) {
            ImageDownloadHelper.getInstance().getOneOfImage(
                    poolTag = tag,
                    urls = imageUrls,
                    reqWidth = reqImageWidth, reqHeight = reqImageHeight,
                    minWidth = Constants.MIN_IMAGE_WIDTH, minHeight = Constants.MIN_IMAGE_HEIGHT,
                    timeoutMs = 3000,
                    imageObserver = imageObserver
            )
        }

        private fun downloadPreview(url: String, imageObserver: ImageDownloadHelper.ImageObserver) {
            ImageDownloadHelper.getInstance().getImageAsync(
                    poolTag = tag + "_thumb",
                    url = url,
                    timeoutMs = 2000,
                    imageObserver = imageObserver
            )
        }

        private fun getMaxAllowSizePreviewNum(
                images: List<ImageData>, maxImageWidth: Int, minImageWidth: Int
        ): Int {
            images.forEachIndexed { i, preview ->
                if (preview.width <= maxImageWidth) {
                    return if (preview.width >= minImageWidth || i == 0) i else i - 1
                }
            }

            return images.size - 1  // returns index of image with min size
        }
    }
}