package com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.LiveData
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stacktivity.yandeximagesearchengine.R.string
import com.stacktivity.yandeximagesearchengine.R.color
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.ui.adapter.SimpleImageListAdapter
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.ImageParser
import com.stacktivity.yandeximagesearchengine.util.FileWorker
import com.stacktivity.yandeximagesearchengine.util.ImageDownloadHelper
import com.stacktivity.yandeximagesearchengine.util.getColor
import com.stacktivity.yandeximagesearchengine.util.getString
import com.stacktivity.yandeximagesearchengine.util.shortToast
import kotlinx.android.synthetic.main.item_image_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * @param maxImageWidth - max preferred width resolution of image
 */
class ImageItemViewHolder(
    itemView: View,
    private val eventListener: EventListener,
    private val maxImageWidth: Int
) : RecyclerView.ViewHolder(itemView), SimpleImageListAdapter.EventListener {

    private val tag = ImageItemViewHolder::class.java.simpleName

    interface EventListener {
        fun onImageLoadFailed(item: ImageItem)
        fun onAdditionalImageClick(imageUrl: String)
    }

    interface ContentProvider {
        fun getImageRealSourceSite(
            possibleSource: String,
            onAsyncResult: (realSource: String?, errorMsg: String?) -> Unit
        )

        fun setAddItemList(list: List<String>)
        fun getAddItemCount(): Int
        fun getAddItemOnPosition(position: Int): String
        fun deleteAddItem(item: String): Int
    }

    private lateinit var item: ImageItem
    private lateinit var contentProvider: ContentProvider
    private var currentPreviewNum: Int = -1
    private var previewColor = getColor(itemView.context, color.colorImagePreview)
    private val otherImageListAdapter = SimpleImageListAdapter(
        this, previewColor
    )
    private var isShownOtherImages = false
    private val downloadImageTimeout = 3000
    private var parserJob: Job? = null
    private var imageDownloadWorkInfoLiveData: LiveData<WorkInfo>? = null

    init {
        itemView.other_image_list_rv.adapter = otherImageListAdapter
    }

    fun bind(
        item: ImageItem,
        bufferFile: File,
        contentProvider: ContentProvider,
        parentWidth: Int
    ) {
        this.item = item
        this.contentProvider = contentProvider
        reset(bufferFile.path)
        bindTextViews(item.dups[0])

        if (bufferFile.exists()) {
            val imageBitmap = BitmapUtils.getBitmapFromFile(bufferFile)
            if (imageBitmap != null) {
                prepareImageView(parentWidth, imageBitmap.width, imageBitmap.height)
                applyBitmapToView(imageBitmap)

                return
            }
        }

        currentPreviewNum = getMaxAllowSizePreviewNum(maxImageWidth, maxImageWidth / 2)  // TODO get width from settings
        Log.d(tag, "bind item: $item")
        val cropFactor = maxImageWidth.toFloat() / item.dups[currentPreviewNum].width
        val reqWidth = maxImageWidth
        val reqHeight = (item.dups[currentPreviewNum].height * cropFactor).toInt()
        prepareImageView(parentWidth, reqWidth, reqHeight)

        downloadBitmap(bufferFile, reqWidth = reqWidth) { imageBitmap ->
            if (imageBitmap != null) {
                prepareImageView(parentWidth, imageBitmap.width, imageBitmap.height)
                applyBitmapToView(imageBitmap)
            } else {
                eventListener.onImageLoadFailed(item)
            }
        }
    }

    override fun onImagesLoadFailed() {
        shortToast(string.images_load_failed)
        resetOtherImagesView()
    }

    override fun onItemClick(item: String) {
        eventListener.onAdditionalImageClick(item)
    }

    private fun reset(otherImagesBufferFileBase: String) {
//        imageDownloadWork?.let { WorkManager.getInstance(itemView.context).cancelWorkById(it.id) }
        imageDownloadWorkInfoLiveData?.removeObservers(itemView.context as LifecycleOwner)
        resetOtherImagesView()
        parserJob?.cancel()
        currentPreviewNum = -1
        val keyFile = File("${otherImagesBufferFileBase}_list")

        itemView.setOnClickListener {
            if (isShownOtherImages) {
                resetOtherImagesView()
            } else {
                itemView.setBackgroundColor(getColor(itemView.context, color.itemOnClickOutSideColor))
                isShownOtherImages = true
                if (keyFile.exists()) {  // Data has already been loaded before, load from cache
                    Log.d(tag, "load other images from buffer")

                    showOtherImages(otherImagesBufferFileBase)
                } else {  // Getting real source of origin image and list of images
                    itemView.progress_bar.visibility = View.VISIBLE
                    contentProvider.getImageRealSourceSite(item.sourceSite) { realSource, errorMsg ->
                        Log.d(tag, "parent: $realSource")
                        if (realSource != null) {
                            parserJob = GlobalScope.launch(Dispatchers.Main) {
                                val imageLinkList = ImageParser.getUrlListToImages(realSource)
                                contentProvider.setAddItemList(imageLinkList)
                                showOtherImages(otherImagesBufferFileBase)
                                FileWorker.createFile(keyFile)
                                Handler(Looper.getMainLooper()).post {
                                    itemView.progress_bar.visibility = View.GONE
                                }
                            }
                        } else {
                            shortToast(getString(string.images_load_failed) + errorMsg)
                            resetOtherImagesView()
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
            shortToast(string.other_images_not_found)
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
        otherImageListAdapter.setNewContentProvider(object :
            SimpleImageListAdapter.ContentProvider {
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

    private fun downloadBitmap(
        bufferFile: File,
        reqWidth: Int? = null,
        reqHeight: Int? = null,
        onAsyncResult: (bitmap: Bitmap?) -> Unit
    ) {
        val imageUrls: Array<String> = (
                item.dups.slice(0..currentPreviewNum).reversed() +
                        item.dups.slice(currentPreviewNum + 1 until item.dups.size)
                ).map { x -> x.url }.toTypedArray()
        val imageDownloadWork = ImageDownloadHelper.getWorkForLoadAndSaveImage(
            imageUrls,
            bufferFile,
            reqWidth,
            reqHeight,
            downloadImageTimeout
        )

        // Start image download
        WorkManager.getInstance(itemView.context).enqueue(imageDownloadWork)

        imageDownloadWorkInfoLiveData =
            WorkManager.getInstance(itemView.context).getWorkInfoByIdLiveData(imageDownloadWork.id)
        imageDownloadWorkInfoLiveData?.observe(itemView.context as LifecycleOwner,
            Observer {
                Log.d(tag, "Download status: ${it.state}")
                when (it.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val imageBitmap = BitmapUtils.getBitmapFromFile(bufferFile)
                        onAsyncResult(imageBitmap)
                    }

                    WorkInfo.State.FAILED -> {
                        onAsyncResult(null)
                    }

                    else -> { }
                }
            }
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

    private fun prepareImageView(parentWidth: Int, imageWidth: Int, imageHeight: Int) {
        val calcImageViewWidth: Float = parentWidth.toFloat() -
                (itemView.image.parent as ViewGroup).run {
                    paddingLeft + paddingRight
                } * 2
        itemView.image.run {
            val cropFactor: Float = calcImageViewWidth / imageWidth
            val cropHeight: Int = (cropFactor * imageHeight).toInt()
            layoutParams.height = cropHeight
            setColorFilter(previewColor)
        }
    }

    private fun bindTextViews(preview: ImageData) {
        val imageResolutionText = "resolution : ${preview.width}x${preview.height}"
        val imageSizeText = "size: ${preview.fileSizeInBytes / 1024}Kb"
        val linkUrl = "${getString(string.action_open_origin_image)}: ${preview.url}"
        val linkSourceUrl = "${getString(string.action_open_origin_image_source)}: ${item.sourceSite}"
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