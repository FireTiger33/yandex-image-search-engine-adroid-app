package com.stacktivity.yandeximagesearchengine.ui.adapter

import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.ImageItemViewHolder
import com.stacktivity.yandeximagesearchengine.util.FileWorker
import com.stacktivity.yandeximagesearchengine.util.ImageParser
import com.stacktivity.yandeximagesearchengine.util.getColor
import com.stacktivity.yandeximagesearchengine.util.getString
import com.stacktivity.yandeximagesearchengine.util.shortToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

internal class ImageListAdapter(
        private val contentProvider: ContentProvider,
        private val imageBufferFilesDir: File,
        private val eventListener: ImageItemViewHolder.EventListener,
        private var maxImageWidth: Int
) : RecyclerView.Adapter<ImageItemViewHolder>() {

    private var parentWidth: Int = 0
    private val bufferedAddImages: MutableMap<Int, Job> = HashMap()
    private val selectedArray: SparseBooleanArray = SparseBooleanArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageItemViewHolder {
        parentWidth = parent.measuredWidth
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_image_list, parent, false)
        val vh = ImageItemViewHolder(view, eventListener)
        vh.innerRecyclerView.apply {
            adapter = SimpleImageListAdapter()
        }
        vh.itemView.setOnClickListener {
            val itemNum = vh.getItemNum()
            val keyFile = File("${imageBufferFilesDir.path + File.separator + itemNum}_list")
            val isClicked = !selectedArray.get(itemNum, false)

            selectedArray.put(itemNum, isClicked)
            onItemClicked(vh, isClicked)

            if (isClicked /*TODO && images is saved*/) {
                if (bufferedAddImages.contains(itemNum)) {  // TODO
                    Log.d(ImageItemViewHolder.tag, "load other images from buffer")
                    showOtherImages(vh)
                } else {  // Getting real source of origin image and list of images
                    vh.showProgressBar()
                    contentProvider.getImageRealSourceSite(vh.item) { realSource, errorMsg ->
                        Log.d(ImageItemViewHolder.tag, "source: ${vh.item.sourceSite} realSource: $realSource")
                        if (realSource != null) {
                            bufferedAddImages[itemNum] = GlobalScope.launch(Dispatchers.Main) {
                                val imageLinkList = ImageParser.getUrlListToImages(realSource)
                                contentProvider.setAddImageList(itemNum, imageLinkList)
                                showOtherImages(vh)
                                FileWorker.createFile(keyFile)
                                vh.hideProgressBar()
                            }
                        } else {
                            shortToast(getString(R.string.images_load_failed) + errorMsg)
                            onItemClicked(vh, false)
                        }
                    }
                }
            }
        }

        (vh.innerRecyclerView.adapter as SimpleImageListAdapter).apply {
            setNewContentProvider(object :  // TODO
                    SimpleImageListAdapter.ContentProvider {
                override fun getItemCount(): Int {
                    return contentProvider.getAddImagesCountOnPosition(vh.getItemNum())
                }

                override fun getItemOnPosition(position: Int): String {
                    return contentProvider.getAddImageListItemOnPosition(vh.getItemNum(), position)
                }

                override fun deleteItem(imageUrl: String): Int {
                    return contentProvider.deleteItemOtherImageOnPosition(vh.getItemNum(), imageUrl)
                }
            })

            eventListener = object : SimpleImageListAdapter.EventListener {
                override fun onImagesLoadFailed() {
                    onItemClicked(vh, false)
                    selectedArray.put(vh.getItemNum(), false)
                }

                override fun onItemClick(item: String) {
                    this@ImageListAdapter.eventListener.onAdditionalImageClick(item)
                }
            }
        }

        return vh

    }


    private fun onItemClicked(vh: ImageItemViewHolder, isClicked: Boolean) {  // TODO rename fun
        if (isClicked) {
            vh.itemView.setBackgroundColor(
                    getColor(vh.itemView.context, R.color.itemOnClickOutSideColor)
            )
        } else {
            vh.itemView.background = null
            vh.hideProgressBar()
            vh.innerRecyclerView.visibility = View.GONE
        }
    }

    private fun showOtherImages(vh: ImageItemViewHolder) {
        if (contentProvider.getAddImagesCountOnPosition(vh.getItemNum()) > 0) {
            vh.innerRecyclerView.visibility = View.VISIBLE
        } else {
            onItemClicked(vh, false)
            shortToast(R.string.other_images_not_found)
        }
    }

    fun onDataClear() {
        selectedArray.clear()
        notifyDataSetChanged()
    }

    fun onChangeScreenConfiguration(maxImageWidth: Int) {
        this.maxImageWidth = maxImageWidth
    }

    override fun getItemCount() = contentProvider.getItemCount()

    override fun onBindViewHolder(holder: ImageItemViewHolder, positionVh: Int) {
        val cacheDir = imageBufferFilesDir.path + File.separator
        val item = contentProvider.getItemOnPosition(positionVh)
        onItemClicked(holder, selectedArray.get(item.itemNum, false))
        holder.maxImageWidth = maxImageWidth
        holder.bind(
                item,
                File(cacheDir + positionVh)
        )
        (holder.innerRecyclerView.adapter as SimpleImageListAdapter?)?.apply {
            this.cacheDir = cacheDir
            notifyDataSetChanged()
        }

        if (selectedArray.get(positionVh, false)) {
            showOtherImages(holder)
        }
    }

    interface ContentProvider {
        fun getItemCount(): Int
        fun getItemOnPosition(position: Int): ImageItem
        fun getImageRealSourceSite(item: ImageItem, onAsyncResult: (realSource: String?, errorMsg: String?) -> Unit)
        fun setAddImageList(position: Int, list: List<String>)
        fun getAddImagesCountOnPosition(position: Int): Int
        fun getAddImageListItemOnPosition(position: Int, itemIndex: Int): String
        fun deleteItemOtherImageOnPosition(position: Int, imageUrl: String): Int
    }
}