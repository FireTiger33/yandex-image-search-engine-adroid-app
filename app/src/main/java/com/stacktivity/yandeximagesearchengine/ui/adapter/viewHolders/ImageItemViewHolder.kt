package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.data.model.Preview
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem
import com.stacktivity.yandeximagesearchengine.ui.adapter.SimpleImageListAdapter
import com.stacktivity.yandeximagesearchengine.util.*
import kotlinx.android.synthetic.main.item_image_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


/**
 * @param maxImageWidth - max preferred width resolution of image
 * @param defaultColor - color of preview image during loading
 */
class ImageItemViewHolder(
    itemView: View,
    private val eventListener: EventListener,
    private val maxImageWidth: Int,
    private val defaultColor: Int
) : RecyclerView.ViewHolder(itemView), SimpleImageListAdapter.EventListener {

    interface EventListener {
        fun onImageLoadFailed(item: SerpItem)
    }

    private lateinit var item: SerpItem
    private var currentPreviewNum: Int = -1
    private val otherImageListAdapter = SimpleImageListAdapter(this)
    private var isShownOtherImages = false
    private val downloadImageTimeout = 3000
    private var job: Job? = null
    private var parserJob: Job? = null

    init {
        itemView.other_image_list_rv.adapter = otherImageListAdapter
    }

    fun bind(item: SerpItem) {
        this.item = item
        reset()
        Log.d("bind", "item: $item")
        var preview = getNextPreview()!!
        bindTextViews(preview)
        preview = getMaxAllowSizePreview(preview)
        prepareImageView(preview.w, preview.h)

        job = GlobalScope.launch(Dispatchers.Main) {
            var imageBitmap: Bitmap? = getImageBitmap(preview)
            var previewHasChanged = false
            val anotherPreviewBitmap: Pair<Preview?, Bitmap?>

            if (imageBitmap == null) {
                anotherPreviewBitmap = getAnotherBitmap()
                if (anotherPreviewBitmap.second != null) {
                    preview = anotherPreviewBitmap.first ?: preview
                    imageBitmap = anotherPreviewBitmap.second
                } else {
                    eventListener.onImageLoadFailed(item)
                    return@launch
                }
                previewHasChanged = true
            }

            Log.d("ImageViewHolder",
                "apply: ${preview.origin?.url
                    ?: preview.url}, currentPreview: $currentPreviewNum, currentDup: ${currentPreviewNum - item.preview.size}"
            )

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

    override fun onImagesLoadFailed() {
        shortToast(R.string.images_load_failed)
        resetOtherImagesView()
    }

    private fun reset() {
        Log.d("ImageViewHolder", "maxImageWidth: $maxImageWidth")
        resetOtherImagesView()
        job?.cancel()
        parserJob?.cancel()
        currentPreviewNum = -1

        itemView.setOnClickListener {
            if (isShownOtherImages) {
                resetOtherImagesView()
                otherImageListAdapter.clearImageList()
            } else {
                itemView.setBackgroundColor(defaultColor)  // TODO databinding
                isShownOtherImages = true
                parserJob = GlobalScope.launch(Dispatchers.Main) {
                    val parentSiteUrl = YandexImageUtil.getImageSourceSite(item)
                    if (parentSiteUrl != null) {
                        val imageLinkList = ImageParser.getUrlListToImages(parentSiteUrl)

                        Log.d("imageList", imageLinkList.toString())

                        if (imageLinkList.isNotEmpty()) {
                            otherImageListAdapter.setNewLinkListToImages(imageLinkList)
                            itemView.other_image_list_rv.visibility = View.VISIBLE
                        } else {
                            itemView.background = null
                            shortToast(R.string.other_images_not_found)
                        }

                    } else {
                        longToast(R.string.yandex_bot_error)
                    }
                }
            }
        }
    }

    private fun resetOtherImagesView() {
        itemView.background = null
        isShownOtherImages = false
        itemView.other_image_list_rv.visibility = View.GONE
    }


    /**
     * Attempt to load another image clone if the corresponding one failed to load.
     *
     * @return Pair<?, null> if attempt failed
     * @return Pair of preview from which the bitmap was obtained and bitmap itself if
     * download is successful
     */
    private suspend fun getAnotherBitmap(): Pair<Preview?, Bitmap?> {
        val previewNum = currentPreviewNum
        var currentPreview: Preview?
        var imageBitmap: Bitmap? = null

        while (getPrevPreview().also { currentPreview = it } != null && imageBitmap == null) {
            if (currentPreview != null) {
                imageBitmap = getImageBitmap(currentPreview!!)
            }
        }
        if (imageBitmap == null) {
            currentPreviewNum = previewNum
            while (getNextPreview().also { currentPreview = it } != null && imageBitmap == null) {
                imageBitmap = getImageBitmap(currentPreview!!)
            }
        }

        return currentPreview to imageBitmap
    }

    private fun getMaxAllowSizePreview(currentPreview: Preview): Preview {
        var preview = currentPreview
        var nullablePreview = getNextPreview()
        while (preview.w > maxImageWidth && nullablePreview != null) {
            nullablePreview = getNextPreview()
            if (nullablePreview != null) {
                preview = nullablePreview
            }
        }

        return preview
    }

    private fun getNextPreview(): Preview? {
        var preview: Preview? = null
        val previewCount = item.preview.size
        val dupsPreviewCount = item.dups.size
        val currentPossibleDupsPreviewNum = currentPreviewNum - previewCount + 1

        if (currentPreviewNum < previewCount - 1) {
            preview = item.preview[++currentPreviewNum]
        } else if (currentPossibleDupsPreviewNum < dupsPreviewCount - 1) {
            preview = item.dups[currentPossibleDupsPreviewNum]
            ++currentPreviewNum
        }

        return preview
    }

    private fun getPrevPreview(): Preview? {
        Log.d("ImageItemViewHolder", "getPrevPreview")
        var preview: Preview? = null
        val previewCount = item.preview.size

        if (currentPreviewNum >= previewCount - 1) {
            preview = item.dups[--currentPreviewNum]
        } else if (currentPreviewNum > 0) {
            preview = item.preview[--currentPreviewNum]
        }

        return preview
    }

    private suspend fun getImageBitmap(preview: Preview): Bitmap? {
        val imageUrl: String = preview.origin?.url ?: preview.url
        return ImageDownloadHelper.getBitmapAsync(imageUrl, downloadImageTimeout)
    }

    private fun prepareImageView(width: Int, height: Int) {
        itemView.image.run {
            val cropFactor: Float = maxImageWidth.toFloat() / width
            val cropHeight: Int = (cropFactor * height).toInt()
            layoutParams.height = cropHeight
            setColorFilter(defaultColor)
        }
    }

    private fun bindTextViews(preview: Preview) {
        val imageResolutionText = "resolution : ${preview.w}x${preview.h}"
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