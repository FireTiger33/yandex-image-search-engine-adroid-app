package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import com.stacktivity.yandeximagesearchengine.base.BaseImageViewHolder
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_HEIGHT
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_WIDTH
import com.stacktivity.yandeximagesearchengine.util.image.ImageProvider
import com.stacktivity.yandeximagesearchengine.util.image.ImageObserver
import kotlinx.android.synthetic.main.simple_item_image_list.view.gifView
import pl.droidsonroids.gif.GifDrawable

internal class SimpleImageViewHolder(
    itemView: View,
    private val eventListener: EventListener
) : BaseImageViewHolder(itemView) {
    val tag: String = SimpleImageViewHolder::class.java.simpleName
    private val viewHeight = itemView.gifView.layoutParams.height
    private var imageObserver: CustomImageObserver? = null

    private abstract class CustomImageObserver(val myUrl: String) : ImageObserver() {
        var requiredToShow = true
    }

    fun bind(imageUrl: String, imageProvider: ImageProvider<String>) {
        reset()

        // val imageBitmap = BitmapUtils.getSimplifiedBitmap(cacheFile.path, reqHeight = viewHeight)
        imageObserver = getImageObserver(imageUrl)
        imageProvider.getImage(imageUrl, imageObserver!!)
    }

    private fun reset() {
        imageObserver?.requiredToShow = false
        prepareImageView(viewHeight, viewHeight)
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

    private fun applyBitmapToView(imageBitmap: Bitmap) {
        prepareImageView(imageBitmap.width, imageBitmap.height)
        itemView.gifView.apply {
            setImageBitmap(imageBitmap)
            clearColorFilter()
        }
    }

    private fun applyGifToView(drawable: GifDrawable, gifWidth: Int, gifHeight: Int) {
        prepareImageView(gifWidth, gifHeight)
        itemView.gifView.run {
            setImageDrawable(drawable)
            clearColorFilter()
        }
    }

    private fun incorrectImageResolution(width: Int, height: Int): Boolean =
        width < MIN_IMAGE_WIDTH
                || height < MIN_IMAGE_HEIGHT
                || width.toFloat() / height > 2.5  // exclude logos

    private fun getImageObserver(imageUrl: String): CustomImageObserver {
        return object : CustomImageObserver(imageUrl) {
            override fun onBitmapResult(bitmap: Bitmap) {
                if (incorrectImageResolution(bitmap.width, bitmap.height)) {
                    eventListener.onSmallImage(myUrl)
                } else if (requiredToShow) {
                    val resBitmap = getOptimizedImage(bitmap)
                    applyBitmapToView(resBitmap)
                }
            }

            override fun onGifResult(drawable: GifDrawable, width: Int, height: Int) {
                if (incorrectImageResolution(width, height)) {
                    eventListener.onSmallImage(myUrl)
                } else if (requiredToShow) {
                    applyGifToView(drawable, width, height)
                }
            }

            override fun onException(e: Throwable) {
                eventListener.onImageLoadFailed(myUrl)
            }
        }
    }

    private fun getOptimizedImage(imageBitmap: Bitmap): Bitmap {
        var res: Bitmap = imageBitmap

        if (imageBitmap.height > viewHeight) {  // reduce image size to improve performance
            val reqWidth = calculateViewWidth(viewHeight, imageBitmap.width, imageBitmap.height)

            res = Bitmap.createScaledBitmap(
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