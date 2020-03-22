package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.stacktivity.yandeximagesearchengine.base.BaseImageViewHolder
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_HEIGHT
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_WIDTH
import com.stacktivity.yandeximagesearchengine.util.ImageDownloadHelper
import com.stacktivity.yandeximagesearchengine.util.observeOnce
import kotlinx.android.synthetic.main.item_image_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class SimpleImageViewHolder(
    itemView: View,
    private val eventListener: EventListener,
    private val defaultColor: Int
) : BaseImageViewHolder(itemView) {
    val tag = SimpleImageViewHolder::class.java.simpleName

    fun bind(imageUrl: String, bufferFile: File?) {
        reset()

        if (bufferFile != null && bufferFile.exists()) {
            val reqHeight = itemView.image.layoutParams.height
            val imageBitmap = BitmapUtils.getSimplifiedBitmap(bufferFile.path, reqHeight = reqHeight)
            if (imageBitmap != null) {
                applyBitmapToView(imageBitmap)
                return
            }
        }
        prepareImageView()

        val imageBitmapLiveData = ImageDownloadHelper.getInstance().getBitmapAsync(tag, imageUrl, timeoutMs = 3000)
        bitmapObserver = object : BitmapObserver() {
            override fun onChanged(bitmap: Bitmap?) {
                var imageBitmap = bitmap
                if (imageBitmap != null) {
                    if (imageBitmap.width < MIN_IMAGE_WIDTH || imageBitmap.height < MIN_IMAGE_HEIGHT) {
                        eventListener.onSmallImage(imageUrl)
                    } else {
                        val imageViewHeight = itemView.image.layoutParams.height
                        if (imageBitmap.height > imageViewHeight) {
                            val cropFactor = imageViewHeight / imageBitmap.height.toFloat()
                            val reqWidth = (imageBitmap.width * cropFactor).toInt()
                            imageBitmap = Bitmap.createScaledBitmap(
                                imageBitmap,
                                reqWidth, imageViewHeight,
                                false
                            ) ?: imageBitmap
                        }

                        GlobalScope.launch(Dispatchers.IO) {
                            if (bufferFile != null) {
                                BitmapUtils.saveBitmapToFile(imageBitmap, bufferFile)
                            }
                        }

                        if (requiredToShow) {
                            applyBitmapToView(imageBitmap)
                        }
                    }
                } else {
                    eventListener.onImageLoadFailed(imageUrl)
                }

                imageBitmapLiveData.removeObserver(this)
            }

        }
        imageBitmapLiveData.observeOnce(itemView.context as LifecycleOwner, bitmapObserver)
    }

    private fun reset() {
        bitmapObserver.requiredToShow = false
    }

    private fun prepareImageView(bitmapWidth: Int? = null, bitmapHeight: Int? = null) {
        itemView.image.apply {
            if (bitmapWidth != null && bitmapHeight != null) {
                val cropFactor: Float = height / bitmapHeight.toFloat()
                val cropWidth: Int = (cropFactor * bitmapWidth).toInt()
                layoutParams.width = cropWidth
            }
            setColorFilter(defaultColor)
        }
    }

    private fun applyBitmapToView(imageBitmap: Bitmap) {
        prepareImageView(imageBitmap.width, imageBitmap.height)
        itemView.image.run {
            setImageBitmap(imageBitmap)
            colorFilter = null
        }
    }

    interface EventListener {
        fun onImageLoadFailed(imageUrl: String)
        fun onSmallImage(imageUrl: String)
    }
}