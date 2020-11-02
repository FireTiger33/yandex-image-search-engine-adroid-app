package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R.color
import com.stacktivity.yandeximagesearchengine.R.string
import com.stacktivity.yandeximagesearchengine.base.BaseImageViewHolder
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.ImageItemLoader
import com.stacktivity.yandeximagesearchengine.util.getString
import kotlinx.android.synthetic.main.item_image_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.droidsonroids.gif.GifDrawable
import java.io.File

internal class ImageItemViewHolder(
        itemView: View,
        private val eventListener: EventListener
) : BaseImageViewHolder(itemView) {
    var maxImageWidth: Int = 0
    val innerRecyclerView: RecyclerView
        get() = itemView.other_image_list_rv

    interface EventListener {
        fun onLoadFailed(itemNum: Int)
    }
    private abstract class ImageObserver : ImageItemLoader.ImageObserver() {
        var requiredToShow = true
        override fun onBitmapResult(bitmap: Bitmap?) {}
        override fun onGifResult(drawable: GifDrawable) {}
    }

    companion object {
        val tag: String = ImageItemViewHolder::class.java.simpleName
    }

    lateinit var item: ImageItem
        private set
    val itemNum: Int
        get() = item.itemNum
    private var imageObserver: ImageObserver? = null
    private var previewImageObserver: ImageObserver? = null

    fun bind(item: ImageItem, bufferFile: File? = null) {
        this.item = item
        reset()
        bindTextViews(item.dups[0])
        showProgressBar()

        previewImageObserver = getPreviewImageObserver()
        imageObserver = getImageObserver()
        ImageItemLoader.getImage(
                item = item,
                reqImageWidth = maxImageWidth,
                minImageWidth = maxImageWidth / 2,
                previewImageObserver = previewImageObserver,
                imageObserver = imageObserver!!,
                cacheFile = bufferFile
        ) { width, height ->
            withContext(Dispatchers.Main) {
                prepareImageView(width, height)
            }
        }
    }

    private fun showProgressBar() {
        itemView.image_load_progress_bar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        itemView.image_load_progress_bar.visibility = View.GONE
    }

    fun showAdditionalProgressBar() {
        itemView.progress_bar.visibility = View.VISIBLE
    }

    fun hideAdditionalProgressBar() {
        itemView.progress_bar.visibility = View.GONE
    }

    private fun reset() {
        imageObserver?.requiredToShow = false
        itemView.gifView.setImageResource(color.colorImagePreview)
    }

    private fun getImageObserver(): ImageObserver {
        return object : ImageObserver() {
            override fun onBitmapResult(bitmap: Bitmap?) {
                if (bitmap != null) {
                    previewImageObserver?.requiredToShow = false
                    if (requiredToShow) {
                        applyBitmapToView(bitmap)
                        hideProgressBar()
                    }
                } else {
                    Log.d(tag, "load failed: $itemNum")
                    eventListener.onLoadFailed(itemNum)
                }
            }

            override fun onGifResult(drawable: GifDrawable) {
                previewImageObserver?.requiredToShow = false
                if (requiredToShow) {
                    applyGifToView(drawable)
                    hideProgressBar()
                }
            }
        }
    }

    private fun applyThumbToView(thumb: Bitmap) {
        itemView.gifView.setImageBitmap(BitmapUtils.blur(thumb))
        itemView.gifView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
    }

    private fun applyBitmapToView(imageBitmap: Bitmap) {
        prepareImageView(imageBitmap.width, imageBitmap.height)
        itemView.gifView.setImageBitmap(imageBitmap)
        itemView.gifView.clearColorFilter()
    }

    private fun applyGifToView(drawable: GifDrawable) {
        itemView.gifView.setImageDrawable(drawable)
        itemView.gifView.clearColorFilter()
    }

    private fun getPreviewImageObserver(): ImageObserver {
        return object : ImageObserver() {
            override fun onBitmapResult(bitmap: Bitmap?) {
                Log.d(tag, "preview is downloaded ${item.itemNum}")
                if (bitmap != null) {
                    if (requiredToShow) {
                        applyThumbToView(bitmap)
                    }
                } else {
                    Log.e(tag, "Thumb load failed")
                }
            }
        }
    }

    private fun prepareImageView(imageWidth: Int, imageHeight: Int) {
        val imageViewWidth: Int = maxImageWidth -
                (itemView.image_container.parent as ViewGroup).run {
                    paddingLeft + paddingRight
                } * 2
        val calcImageViewHeight = calculateViewHeight(imageViewWidth, imageWidth, imageHeight)
        itemView.gifView.run {
            clearAnimation()
            layoutParams.height = calcImageViewHeight
            requestLayout()
        }
        itemView.image_container.run {
            layoutParams.height = calcImageViewHeight
            requestLayout()
        }
    }

    private fun bindTextViews(preview: ImageData) {
        val imageResolutionText = "resolution : ${preview.width}x${preview.height}"
        val imageSizeText = "size: ${preview.fileSizeInBytes / 1024}Kb"
        val linkUrl = "${getString(string.action_open_origin_image)}: ${preview.url}"
        val linkSourceUrl =
                "${getString(string.action_open_origin_image_source)}: ${item.sourceSite}"
        itemView.run {
            title.text = item.title
            image_resolution.text = imageResolutionText
            image_size.text = imageSizeText
            link.text = linkUrl
            link.movementMethod = LinkMovementMethod.getInstance()
            link_source.text = linkSourceUrl
            link_source.movementMethod = LinkMovementMethod.getInstance()
        }
    }
}