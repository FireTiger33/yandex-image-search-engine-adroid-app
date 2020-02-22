package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.App
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.data.model.ImageData
import com.stacktivity.yandeximagesearchengine.data.model.ImageItem
import com.stacktivity.yandeximagesearchengine.ui.adapter.SimpleImageListAdapter
import com.stacktivity.yandeximagesearchengine.util.*
import kotlinx.android.synthetic.main.item_image_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File


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

    private val tag = ImageItemViewHolder::class.java.simpleName

    interface EventListener {
        fun onImageLoadFailed(item: ImageItem)
    }

    interface ContentProvider {
        fun setAddItemList(list: List<String>)
        fun getAddItemCount(): Int
        fun getAddItemOnPosition(position: Int): String
        fun deleteAddItem(item: String): Int
    }

    private lateinit var item: ImageItem
    private lateinit var contentProvider: ContentProvider
    private var currentPreviewNum: Int = -1
    private val otherImageListAdapter = SimpleImageListAdapter(
        this, defaultColor
    )
    private var isShownOtherImages = false
    private val downloadImageTimeout = 3000
    private var job: Job? = null
    private var parserJob: Job? = null

    init {
        itemView.other_image_list_rv.adapter = otherImageListAdapter
    }

    fun bind(item: ImageItem, bufferFile: File, contentProvider: ContentProvider) {
        this.item = item
        this.contentProvider = contentProvider
        reset(bufferFile.path)
        var preview = getNextPreview()!!
        bindTextViews(preview)
        if (bufferFile.exists()) {
            val imageBitmap = BitmapUtils.getBitmapFromFile(bufferFile)
            if (imageBitmap != null) {
                prepareImageView(imageBitmap.width, imageBitmap.height)
                applyBitmapToView(imageBitmap)
                return
            }
        }

        Log.d(tag, "bind item: $item")
        if (preview.width > maxImageWidth) {
            preview = getMaxAllowSizePreview(preview)
        }
        prepareImageView(preview.width, preview.height)

        job = GlobalScope.launch(Dispatchers.Main) {
            var imageBitmap: Bitmap? = getImageBitmap(preview)
            var previewHasChanged = false
            val anotherPreviewBitmap: Pair<ImageData?, Bitmap?>

            if (imageBitmap == null) {
                anotherPreviewBitmap = getAnotherBitmap()
                if (anotherPreviewBitmap.second != null) {
                    preview = anotherPreviewBitmap.first ?: preview
                    imageBitmap = anotherPreviewBitmap.second
                }
                previewHasChanged = true
            }

            if (imageBitmap != null) {
                BitmapUtils.saveBitmapToFile(imageBitmap, bufferFile)

                Log.d(tag,
                    "apply: ${preview.url}, currentPreview: $currentPreviewNum"
                )
            } else {
                eventListener.onImageLoadFailed(item)
                return@launch
            }

            Handler(Looper.getMainLooper()).post {
                if (previewHasChanged) {
                    prepareImageView(preview.width, preview.height)
                }
                applyBitmapToView(imageBitmap)
            }
        }
    }

    override fun onImagesLoadFailed() {
        shortToast(R.string.images_load_failed)
        resetOtherImagesView()
    }

    private fun reset(otherImagesBufferFileBase: String) {
        resetOtherImagesView()
        job?.cancel()
        parserJob?.cancel()
        currentPreviewNum = -1
        val keyFile = File("${otherImagesBufferFileBase}_list")

        itemView.setOnClickListener {
            if (isShownOtherImages) {
                resetOtherImagesView()
            } else {
                itemView.setBackgroundColor(defaultColor)  // TODO databinding
                isShownOtherImages = true
                if (keyFile.exists()) {
                    Log.d(tag, "load other images from buffer")

                    showOtherImages(otherImagesBufferFileBase)
                } else {
                    itemView.progress_bar.visibility = View.VISIBLE
                    parserJob = GlobalScope.launch(Dispatchers.Main) {
                        val parentSiteUrl = YandexImageUtil.getImageRealSourceSite(item.sourceSite)
                        Log.d(tag, "parent: $parentSiteUrl")

                        if (parentSiteUrl != null) {
                            val imageLinkList = ImageParser.getUrlListToImages(parentSiteUrl)
                            contentProvider.setAddItemList(imageLinkList)
                            showOtherImages(otherImagesBufferFileBase)
                            FileWorker.createFile(keyFile)
                        } else {
                            // TODO show captcha
                            Log.d(tag, "Yandex bot error")
                            resetOtherImagesView()
                            longToast(R.string.yandex_bot_error)
                        }

                        Handler(Looper.getMainLooper()).post {
                            itemView.progress_bar.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun showOtherImages(otherImagesBufferFileBase: String) {
        if (contentProvider.getAddItemCount() > 0) {
            otherImageListAdapter.bufferFileBase = otherImagesBufferFileBase
            itemView.other_image_list_rv.visibility = View.VISIBLE
        } else {
            resetOtherImagesView()
            shortToast(R.string.other_images_not_found)
        }
    }

    private fun applyBitmapToView(imageBitmap: Bitmap) {
        itemView.image.run {
            setImageBitmap(imageBitmap)
            colorFilter = null
        }
    }

    private fun resetOtherImagesView() {
        itemView.background = null
        itemView.progress_bar.visibility = View.GONE
        isShownOtherImages = false
        itemView.other_image_list_rv.visibility = View.GONE
        otherImageListAdapter.setNewContentProvider(object : SimpleImageListAdapter.ContentProvider {
            override fun getItemCount(): Int {
                return contentProvider.getAddItemCount()
            }

            override fun getItemOnPosition(position: Int): String {
                return contentProvider.getAddItemOnPosition(position)
            }

            override fun deleteItem(imageUrl: String): Int {
                return contentProvider.deleteAddItem(imageUrl)
            }
        })
    }

    /**
     * Attempt to load another image clone if the corresponding one failed to load.
     *
     * @return Pair<?, null> if attempt failed
     * @return Pair of preview from which the bitmap was obtained and bitmap itself if
     * download is successful
     */
    private suspend fun getAnotherBitmap(): Pair<ImageData?, Bitmap?> {
        val previewNum = currentPreviewNum
        var currentPreview: ImageData?
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

    private fun getMaxAllowSizePreview(currentPreview: ImageData): ImageData {
        var preview = currentPreview
        var nullablePreview = getNextPreview()

        while (preview.width > maxImageWidth && nullablePreview != null) {
            nullablePreview = getNextPreview()
            if (nullablePreview != null) {
                preview = nullablePreview
            }
        }

        return preview
    }

    private fun getNextPreview(): ImageData? {
        var preview: ImageData? = null
        val previewCount = item.dups.size

        if (currentPreviewNum < previewCount - 1) {
            preview = item.dups[++currentPreviewNum]
        }

        return preview
    }

    private fun getPrevPreview(): ImageData? {
        var preview: ImageData? = null

        if (currentPreviewNum > 0) {
            preview = item.dups[--currentPreviewNum]
        }

        return preview
    }

    private suspend fun getImageBitmap(preview: ImageData): Bitmap? {
        val imageUrl: String = preview.url
        var reqWidth: Int? = null
        var reqHeight: Int? = null
        if (preview.width > maxImageWidth) {  // TODO
            val cropFactor = maxImageWidth.toFloat() / preview.width
            reqWidth = maxImageWidth
            reqHeight = (preview.height * cropFactor).toInt()
        }

        return ImageDownloadHelper.getBitmapAsync(imageUrl, reqWidth, reqHeight, downloadImageTimeout)
    }

    private fun prepareImageView(width: Int, height: Int) {
        itemView.image.run {
            val cropFactor: Float = maxImageWidth.toFloat() / width
            val cropHeight: Int = (cropFactor * height).toInt()
            layoutParams.height = cropHeight
            setColorFilter(defaultColor)
        }
    }

    private fun bindTextViews(preview: ImageData) {
        val imageResolutionText = "resolution : ${preview.width}x${preview.height}"
        val imageSizeText = "size: ${preview.fileSizeInBytes / 1024}Kb"
        val linkUrl = "${App.getInstance().getString(R.string.action_open_origin_image)}: ${preview.url}"
        val linkSourceUrl = "${App.getInstance().getString(R.string.action_open_origin_image_source)}: ${item.sourceSite}"
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