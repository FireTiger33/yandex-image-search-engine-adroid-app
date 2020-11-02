package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import com.stacktivity.yandeximagesearchengine.base.BaseImageViewHolder
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_HEIGHT
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_WIDTH
import com.stacktivity.yandeximagesearchengine.util.FileWorker
import com.stacktivity.yandeximagesearchengine.util.ImageDownloadHelper
import kotlinx.android.synthetic.main.simple_item_image_list.view.gifView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.nio.ByteBuffer

@ExperimentalUnsignedTypes
internal class SimpleImageViewHolder(
        itemView: View,
        private val eventListener: EventListener
) : BaseImageViewHolder(itemView) {
    val tag: String = SimpleImageViewHolder::class.java.simpleName
    private val viewHeight = itemView.gifView.layoutParams.height
    private var imageObserver: ImageObserver? = null

    private abstract class ImageObserver(val myUrl: String) : ImageDownloadHelper.ImageObserver() {
        var requiredToShow = true
    }

    fun bind(imageUrl: String, bufferFile: File? = null) {
        reset()

        if (bufferFile != null && bufferFile.exists()) {
            applyImageFromCache(bufferFile)
        } else {
            Log.d(tag, "view: ${viewHeight}x${viewHeight}")
            prepareImageView(viewHeight, viewHeight)
            imageObserver = getImageObserver(bufferFile, imageUrl)
            downloadImage(imageUrl, imageObserver as ImageObserver)
        }
    }

    private fun reset() {
        imageObserver?.requiredToShow = false
    }

    private fun prepareImageView(bitmapWidth: Int, bitmapHeight: Int) {
        val calcWidth = calculateViewWidth(viewHeight, bitmapWidth, bitmapHeight)
        itemView.gifView.apply {
            Log.d(this@SimpleImageViewHolder.tag, "calculated imageView width = $calcWidth")
            layoutParams.width = calcWidth
            refreshDrawableState()
            setColorFilter(Color.DKGRAY)
            requestLayout()
        }
    }

    private fun applyImageFromCache(cacheFile: File): Boolean {
        var res = false
        if (BitmapUtils.fileIsAnGifImage(cacheFile.path)) {
            applyGifToView(cacheFile)
            res = true
        } else if (BitmapUtils.fileIsAnImage(cacheFile.path)) {
            val imageBitmap =
                    BitmapUtils.getSimplifiedBitmap(cacheFile.path, reqHeight = viewHeight)
            if (imageBitmap != null) {
                applyBitmapToView(imageBitmap)
                res = true
            }
        }

        return res
    }

    private fun applyBitmapToView(imageBitmap: Bitmap) {
        /*itemView.imageView.visibility = View.VISIBLE
        itemView.gifView.visibility = View.GONE*/
        prepareImageView(imageBitmap.width, imageBitmap.height)
        itemView.gifView.apply {
            setImageBitmap(imageBitmap)
            clearColorFilter()
        }
    }

    private fun applyGifToView(buffer: ByteBuffer) {
        val gifSize = getGifSize(buffer)
        prepareImageView(gifSize.first, gifSize.second)
        itemView.gifView.apply {
            setImageDrawable(GifDrawable(buffer))
            clearColorFilter()
        }

    }

    private fun applyGifToView(cacheFile: File) {
        val gifSize = BitmapUtils.getImageSize(cacheFile)
        prepareImageView(gifSize.first, gifSize.second)
        itemView.gifView.apply {
            setImageDrawable(GifDrawable(cacheFile))
            itemView.gifView.clearColorFilter()
        }

    }

    private fun incorrectImage(imageBitmap: Bitmap): Boolean =
            imageBitmap.width < MIN_IMAGE_WIDTH
                    || imageBitmap.height < MIN_IMAGE_HEIGHT
                    || imageBitmap.width.toFloat() / imageBitmap.height > 2.5  // exclude logos

    private fun incorrectGifResolution(buffer: ByteBuffer): Boolean {
        val gifSize = getGifSize(buffer)

        return gifSize.first < MIN_IMAGE_WIDTH || gifSize.second < MIN_IMAGE_HEIGHT
    }

    private fun getImageObserver(bufferFile: File?, imageUrl: String): ImageObserver {
        return object : ImageObserver(imageUrl) {
            override fun onBitmapResult(bitmap: Bitmap?) {
                if (bitmap != null) {
                    if (incorrectImage(bitmap)) {
                        eventListener.onSmallImage(myUrl)
                    } else if (requiredToShow) {
                        val resBitmap = getOptimizedImage(bitmap)

                        Log.d(tag, "image: ${resBitmap.width}x${resBitmap.height}")
                        prepareImageView(resBitmap.width, resBitmap.height)
                        applyBitmapToView(resBitmap)

                        if (bufferFile != null) {
                            BitmapUtils.saveBitmapToFile(resBitmap, bufferFile)
                        }
                    }
                } else {
                    eventListener.onImageLoadFailed(myUrl)
                }
            }

            override fun onGifResult(buffer: ByteBuffer) {
                if (incorrectGifResolution(buffer)) {
                    eventListener.onSmallImage(myUrl)
                } else if (requiredToShow) {
                    applyGifToView(buffer)
                    if (bufferFile != null) {
                        GlobalScope.launch {
                            FileWorker.saveBytesToFile(buffer, bufferFile)
                        }
                    }
                }
            }
        }
    }

    private fun downloadImage(url: String, imageObserver: ImageObserver) {
        ImageDownloadHelper.getInstance().getImageAsync(
                poolTag = tag,
                url = url,
                timeoutMs = 5000,
                imageObserver = imageObserver
        )
    }

    private fun getOptimizedImage(imageBitmap: Bitmap): Bitmap {
        var res: Bitmap = imageBitmap

        if (imageBitmap.height > viewHeight) {  // reduce image size to improve performance
            val reqWidth = calculateViewWidth(viewHeight, imageBitmap.width, imageBitmap.height)

            res =  Bitmap.createScaledBitmap(
                    imageBitmap,
                    reqWidth, viewHeight,
                    false
            ) ?: imageBitmap
        }

        return res
    }

    interface EventListener {
        fun onImageLoadFailed(imageUrl: String)
        fun onSmallImage(imageUrl: String)
    }
}