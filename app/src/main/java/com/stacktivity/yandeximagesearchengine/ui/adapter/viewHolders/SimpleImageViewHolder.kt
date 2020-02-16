package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.stacktivity.yandeximagesearchengine.App
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.util.ImageDownloadHelper
import kotlinx.android.synthetic.main.simple_item_image_list.view.image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SimpleImageViewHolder(
    itemView: View,
    private val eventListener: EventListener
): ViewHolder(itemView) {

    fun bind(imageUrl: String) {
        prepareImageView()
        GlobalScope.launch(Dispatchers.Main) {
            val imageBitmap: Bitmap? = ImageDownloadHelper.getBitmapAsync(imageUrl, 2000)

            if (imageBitmap != null) {
                Handler(Looper.getMainLooper()).post {
                    itemView.image.run {
                        val cropFactor: Float = height / imageBitmap.height.toFloat()
                        val cropWidth: Int = (cropFactor * imageBitmap.width).toInt()
                        layoutParams.width = cropWidth
                        setImageBitmap(imageBitmap)
                        colorFilter = null
                    }
                }
            } else {
                eventListener.onImageLoadFailed(imageUrl)
            }
        }
    }

    private fun prepareImageView() {
        val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            itemView.context.resources.getColor(R.color.colorImagePreview, App.getInstance().theme)
        } else {
            ContextCompat.getColor(itemView.context, R.color.colorImagePreview)
        }
        itemView.image.setColorFilter(color)
    }

    interface EventListener {
        fun onImageLoadFailed(imageUrl: String)
    }
}