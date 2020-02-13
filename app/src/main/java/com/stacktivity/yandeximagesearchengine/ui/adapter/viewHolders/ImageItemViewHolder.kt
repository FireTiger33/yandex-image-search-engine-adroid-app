package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.App
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.data.model.Preview
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem
import com.stacktivity.yandeximagesearchengine.util.ImageDownloadHelper
import kotlinx.android.synthetic.main.item_image_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ImageItemViewHolder(
    itemView: View, private val maxImageWidth: Int
): RecyclerView.ViewHolder(itemView) {

    private lateinit var item: SerpItem
    private var currentPreviewNum: Int = -1
    private var currentDupPreviewNum: Int = -1
    private val downloadImageTimeout = 3000
    private var job: Job? = null

    fun bind(item: SerpItem) {
        reset()
        this.item = item
        Log.d("bind", "item: $item")
        var preview = getNextPreview()!!
        bindTextViews(preview)
        preview = getMaxAllowSizePreview(preview)
        prepareImageView(preview.w, preview.h)

        job = GlobalScope.launch(Dispatchers.Main) {
            var imageBitmap: Bitmap? = getImageBitmap(preview)
            var previewHasChanged = false
            while (imageBitmap == null) {
                preview = getNextPreview()?: preview  // TODO remove view if not available results
                imageBitmap = getImageBitmap(preview)
                previewHasChanged = true
            }
            Log.d("ImageViewHolder", "apply: ${preview.origin?.url ?: preview.url}, currentPreview: $currentPreviewNum, currentDup: $currentDupPreviewNum")

            Handler(Looper.getMainLooper()).post {
                if (previewHasChanged) {
                    prepareImageView(preview.w, preview.h)
                }
                itemView.image.run {
                    setImageBitmap(imageBitmap)
                    colorFilter = null
                }
            }
        }
    }

    private fun reset() {
        Log.d("ImageViewHolder", "maxImageWidth: $maxImageWidth")
        job?.cancel()
        currentPreviewNum = -1
        currentDupPreviewNum = -1
    }

    private fun getMaxAllowSizePreview(currentPreview: Preview): Preview {
        var preview = currentPreview
        while (preview.w > maxImageWidth) {
            val nullablePreview = getNextPreview()
            if (nullablePreview != null) {
                preview = nullablePreview
            }
        }

        return preview
    }

    private fun getNextPreview(): Preview? {
        var preview: Preview? = null

        if (currentPreviewNum < item.preview.size - 1) {
            preview = item.preview[++currentPreviewNum]
        } else if (currentDupPreviewNum < item.dups.size - 1) {
            preview = item.dups[++currentDupPreviewNum]
        }

        return preview
    }

    // TODO getPrevPreview with Bitmap constructor

    private suspend fun getImageBitmap(preview: Preview): Bitmap? {
        val imageUrl: String = preview.origin?.url ?: preview.url
        return ImageDownloadHelper.getBitmapAsync(imageUrl, downloadImageTimeout)
    }

    private fun prepareImageView(width: Int, height: Int) {
        itemView.image.run {
            val cropFactor: Float = maxImageWidth.toFloat() / width
            val cropHeight: Int = (cropFactor * height).toInt()
            layoutParams.height = cropHeight
            val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.resources.getColor(R.color.colorImagePreview, App.getInstance().theme)
            } else {
                ContextCompat.getColor(context, R.color.colorImagePreview)
            }
            setColorFilter(color)
        }
    }

    private fun bindTextViews(preview: Preview) {
        val imageResolutionText = "original size: ${preview.w}x${preview.h}"
        val imageSizeText = "size: ${preview.fileSizeInBytes / 1024}Kb"
        itemView.run {
            title.text = item.snippet.title
            image_resolution.text = imageResolutionText
            image_size.text = imageSizeText
            link.text = preview.origin?.url?: preview.url
            link.movementMethod = LinkMovementMethod.getInstance()
        }
    }
}