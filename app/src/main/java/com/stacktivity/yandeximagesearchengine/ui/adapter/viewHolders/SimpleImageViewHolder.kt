package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_HEIGHT
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_WIDTH
import com.stacktivity.yandeximagesearchengine.util.ImageDownloadHelper
import kotlinx.android.synthetic.main.item_image_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class SimpleImageViewHolder(
    itemView: View,
    private val eventListener: EventListener,
    private val defaultColor: Int
): ViewHolder(itemView) {

    fun bind(imageUrl: String, bufferFile: File?) {
        if (bufferFile != null && bufferFile.exists()) {
            val reqHeight = itemView.image.layoutParams.height
            val imageBitmap = BitmapUtils.getSimplifiedBitmap(bufferFile.path, reqHeight = reqHeight)
            if (imageBitmap != null) {
                prepareImageView(imageBitmap.width, imageBitmap.height)
                applyBitmapToView(imageBitmap)
                return
            }
        }
        prepareImageView()
        GlobalScope.launch(Dispatchers.Main) {
            var imageBitmap: Bitmap? = ImageDownloadHelper.getBitmapAsync(imageUrl, timeoutMs = 2000)

            if (imageBitmap != null) {
                if (imageBitmap.width < MIN_IMAGE_WIDTH || imageBitmap.height < MIN_IMAGE_HEIGHT) {
                    eventListener.onSmallImage(imageUrl)
                } else {
                    val imageViewHeight = itemView.image.layoutParams.height
                    if (imageBitmap.height > imageViewHeight) {
                        val cropFactor = imageViewHeight / imageBitmap.height.toFloat()  // TODO
                        val reqWidth = (imageBitmap.width * cropFactor).toInt()
                        imageBitmap = Bitmap.createScaledBitmap(
                            imageBitmap,
                            reqWidth, imageViewHeight,
                            false
                        )?: imageBitmap
                    }

                    if (bufferFile != null) {
                        BitmapUtils.saveBitmapToFile(imageBitmap, bufferFile)
                    }
                    Handler(Looper.getMainLooper()).post {
                        prepareImageView(imageBitmap.width, imageBitmap.height)
                        applyBitmapToView(imageBitmap)
                    }
                }
            } else {
                eventListener.onImageLoadFailed(imageUrl)
            }
        }
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