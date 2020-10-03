package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
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
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_HEIGHT
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_WIDTH
import com.stacktivity.yandeximagesearchengine.util.ImageDownloadHelper
import com.stacktivity.yandeximagesearchengine.util.getColor
import com.stacktivity.yandeximagesearchengine.util.getString
import kotlinx.android.synthetic.main.item_image_list.view.*
import kotlinx.coroutines.Job
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.nio.ByteBuffer

internal class ImageItemViewHolder(
        itemView: View,
        private val eventListener: EventListener
) : BaseImageViewHolder(itemView) {
    var maxImageWidth: Int = 0
    val innerRecyclerView: RecyclerView
        get() = itemView.other_image_list_rv

    interface EventListener {
        fun onImageLoadFailed(item: ImageItem)
        fun onAdditionalImageClick(imageUrl: String)
    }

    private abstract class ImageObserver : ImageDownloadHelper.ImageObserver() {
        var requiredToShow = true
    }

    companion object {
        val tag = ImageItemViewHolder::class.java.simpleName
    }

    lateinit var item: ImageItem
        private set
    private var currentPreviewNum: Int = -1
    private var previewColor = getColor(itemView.context, color.colorImagePreview)
    private var parserJob: Job? = null
    private var imageObserver: ImageObserver? = null


    fun bind(item: ImageItem, bufferFile: File? = null) {
        this.item = item
        reset()
        bindTextViews(item.dups[0])
        showProgressBar()
        if (bufferFile != null && bufferFile.exists()) {
            if (applyImageFromCache(bufferFile)) {
                hideProgressBar()
                return
            }
        }

        currentPreviewNum = getMaxAllowSizePreviewNum(
                maxImageWidth = maxImageWidth,
                minImageWidth = maxImageWidth / 2
        )  // TODO get width from settings
        val imageWidth = item.dups[currentPreviewNum].width
        val imageHeight = item.dups[currentPreviewNum].height
        prepareImageView(imageWidth, imageHeight)

        imageObserver = getImageObserver(bufferFile)
        downloadImage(
                reqWidth = maxImageWidth,
                imageObserver = imageObserver as ImageObserver
        )
    }

    fun getItemNum(): Int {
        return item.itemNum
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
        parserJob?.cancel()
        currentPreviewNum = -1
    }

    private fun getImageObserver(bufferFile: File?): ImageObserver {
        return object : ImageObserver() {
            override fun onBitmapResult(bitmap: Bitmap?) {
                if (bitmap != null) {
                    if (requiredToShow) {
                        applyBitmapToView(bitmap)
                        hideProgressBar()
                    }

                    if (bufferFile != null) {
                        BitmapUtils.saveBitmapToFile(bitmap, bufferFile)
                    }
                } else {
                    Log.d(tag, "load failed: $item")
                    eventListener.onImageLoadFailed(item)
                }
            }

            override fun onGifResult(buffer: ByteBuffer) {
                if (requiredToShow) {
                    applyGifToView(buffer)
                    hideProgressBar()
                }
                if (bufferFile != null) {
                    saveImageToCache(buffer, bufferFile)
                }
            }
        }
    }

    private fun applyImageFromCache(cacheFile: File): Boolean {
        var res = false

        if (BitmapUtils.fileIsAnGifImage(cacheFile.path)) {
            applyGifToView(cacheFile)
            res = true
        } else if (BitmapUtils.fileIsAnImage(cacheFile.path)) {
            val imageBitmap = BitmapUtils.getBitmapFromFile(cacheFile)

            if (imageBitmap != null) {
                applyBitmapToView(imageBitmap)
                res = true
            }
        }

        return res
    }

    private fun applyBitmapToView(imageBitmap: Bitmap) {
        prepareImageView(imageBitmap.width, imageBitmap.height)
        itemView.gifView.setImageBitmap(imageBitmap)
        itemView.gifView.clearColorFilter()
    }

    private fun applyGifToView(buffer: ByteBuffer) {
        val gifSize = getGifSize(buffer)
        prepareImageView(gifSize.first, gifSize.second)
        itemView.gifView.setImageDrawable(GifDrawable(buffer))
        itemView.gifView.clearColorFilter()
    }

    private fun applyGifToView(cacheFile: File) {
        val gifSize = getImageSize(cacheFile)
        prepareImageView(gifSize.first, gifSize.second)
        itemView.gifView.setImageDrawable(GifDrawable(cacheFile))
        itemView.gifView.clearColorFilter()
    }

    private fun downloadImage(
            reqWidth: Int? = null, reqHeight: Int? = null,
            imageObserver: ImageObserver
    ) {
        val imageUrls: Array<String> = (
                item.dups.slice(0..currentPreviewNum).reversed() +
                        item.dups.slice(currentPreviewNum + 1 until item.dups.size)
                ).map { x -> x.url }.toTypedArray()

        ImageDownloadHelper.getInstance()
                .getOneOfImage(
                        poolTag = tag,
                        urls = imageUrls,
                        reqWidth = reqWidth, reqHeight = reqHeight,
                        minWidth = MIN_IMAGE_WIDTH, minHeight = MIN_IMAGE_HEIGHT,
                        timeoutMs = 3000,
                        imageObserver = imageObserver
                )
    }

    private fun getMaxAllowSizePreviewNum(maxImageWidth: Int, minImageWidth: Int): Int {
        item.dups.forEachIndexed { i, preview ->
            if (preview.width <= maxImageWidth) {
                return if (preview.width >= minImageWidth || i == 0) i else i - 1
            }
        }

        return item.dups.size - 1
    }

    private fun prepareImageView(imageWidth: Int, imageHeight: Int) {
        Log.d(tag, "prepareImageView: $adapterPosition")
        val imageViewWidth: Int = maxImageWidth -
                (itemView.image_container.parent as ViewGroup).run {
                    paddingLeft + paddingRight
                } * 2
        val calcImageViewHeight = calculateViewHeight(imageViewWidth, imageWidth, imageHeight)
        itemView.gifView.run {
            clearAnimation()
            layoutParams.height = calcImageViewHeight
            requestLayout()
            setImageResource(color.colorImagePreview)
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