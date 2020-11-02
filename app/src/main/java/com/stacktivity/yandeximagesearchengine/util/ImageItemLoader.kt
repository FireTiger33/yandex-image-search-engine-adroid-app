package com.stacktivity.yandeximagesearchengine.util

import android.graphics.Bitmap
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import kotlinx.coroutines.*
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.nio.ByteBuffer

class ImageItemLoader {
    abstract class ImageObserver {
        abstract fun onBitmapResult(bitmap: Bitmap?)
        abstract fun onGifResult(drawable: GifDrawable)
    }

    companion object {
        val tag: String = ImageItemLoader::class.java.simpleName

        fun <IObserver : ImageObserver> getImage(
                item: ImageItem,
                reqImageWidth: Int, minImageWidth: Int,
                imageObserver: IObserver,
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

            downloadImage(imageUrls, reqImageWidth, cachingObserver)
        }

        private suspend fun resultFromCache(cacheFile: File, imageObserver: ImageObserver): Boolean {
            var res = false
            val fileExists = withContext(Dispatchers.IO) { cacheFile.exists() }
            if (!fileExists) {
                return false
            }

            if (BitmapUtils.fileIsAnGifImage(cacheFile.path)) {
                val drawable = getDrawableFromFile(cacheFile)
                withContext(Dispatchers.Main) { imageObserver.onGifResult(drawable) }
                res = true
            } else if (BitmapUtils.fileIsAnImage(cacheFile.path)) {
                res = true
                BitmapUtils.getBitmapFromFile(cacheFile) { imageBitmap ->
                    if (imageBitmap != null) withContext(Dispatchers.Main) {
                        imageObserver.onBitmapResult(imageBitmap)
                    }
                }
            }

            return res
        }

        private fun getDrawableFromFile(file: File): GifDrawable {
            return GifDrawable(file)
        }

        private fun getCachingObserver(
                imageObserver: ImageObserver,
                cacheFile: File?
        ): ImageDownloadHelper.ImageObserver {
            return object : ImageDownloadHelper.ImageObserver() {
                override fun onBitmapResult(bitmap: Bitmap?) {
                    imageObserver.onBitmapResult(bitmap)
                    if (cacheFile != null && bitmap != null) {
                        BitmapUtils.saveBitmapToFile(bitmap, cacheFile)
                    }
                }

                override fun onGifResult(buffer: ByteBuffer) {
                    imageObserver.onGifResult(GifDrawable(buffer))
                    if (cacheFile != null) {
                        GlobalScope.launch(Dispatchers.IO) {
                            FileWorker.saveBytesToFile(buffer, cacheFile)
                        }
                    }
                }
            }
        }

        private fun downloadImage(
                imageUrls: List<String>,
                reqImageWidth: Int,
                imageObserver: ImageDownloadHelper.ImageObserver,
        ) {
            ImageDownloadHelper.getInstance().getOneOfImage(
                    poolTag = tag,
                    urls = imageUrls,
                    reqWidth = reqImageWidth,
                    minWidth = Constants.MIN_IMAGE_WIDTH, minHeight = Constants.MIN_IMAGE_HEIGHT,
                    timeoutMs = 3000,
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