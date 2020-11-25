package com.stacktivity.yandeximagesearchengine.ui.adapter

import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.providers.ImageItemsProvider
import com.stacktivity.yandeximagesearchengine.providers.SubImagesProvider
import com.stacktivity.yandeximagesearchengine.util.prefetcher.PrefetchRecycledViewPool
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.ImageItemViewHolder
import com.stacktivity.yandeximagesearchengine.util.getColor
import com.stacktivity.yandeximagesearchengine.util.getString
import com.stacktivity.yandeximagesearchengine.util.image.BufferedImageProvider
import com.stacktivity.yandeximagesearchengine.util.shortToast

internal class ImageListAdapter(
    private val contentProvider: ImageItemsProvider,
    private val subImagesProvider: SubImagesProvider,
    private val imageLoader: BufferedImageProvider<String>,
    private val imageItemLoader: BufferedImageProvider<ImageItem>,
    private var maxImageWidth: Int
) : RecyclerView.Adapter<ImageItemViewHolder>(), ImageItemViewHolder.EventListener {

    private val selectedArray: SparseBooleanArray = SparseBooleanArray()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_image_list, parent, false)
        val vh = ImageItemViewHolder(view, this, imageItemLoader)
        vh.innerRecyclerView.run {
            adapter = SimpleImageListAdapter(imageLoader).apply {
                setNewContentProvider(object :  // TODO
                    SimpleImageListAdapter.ContentProvider {
                    override fun getItemCount(): Int {
                        return subImagesProvider.getSubImagesCount(vh.itemNum)
                    }

                    override fun getItemOnPosition(position: Int): String {
                        return subImagesProvider.getSubImage(vh.itemNum, position)
                    }

                    override fun deleteItem(imageUrl: String): Int {
                        return subImagesProvider.deleteSubImage(vh.itemNum, imageUrl)
                    }
                })

                eventListener = object : SimpleImageListAdapter.EventListener {
                    override fun onImagesLoadFailed() {
                        toggleView(vh, false)
                    }
                }
            }
        }

        vh.itemView.setOnClickListener {
            val itemNum = vh.itemNum

            if (toggleView(vh)) {
                vh.showAdditionalProgressBar()
                subImagesProvider.loadSubImages(itemNum) { success, errorMsg ->
                    vh.hideAdditionalProgressBar()
                    if (success) {
                        showOtherImages(vh)
                    } else {
                        shortToast(getString(R.string.images_load_failed) + errorMsg)
                        toggleView(vh, false)
                    }
                }
            }
        }

        return vh
    }

    fun prefetchViewHolders(viewPool: PrefetchRecycledViewPool) {
        val currentRecycledViews = viewPool.getRecycledViewCount(0)
        if (currentRecycledViews < 8) {
            viewPool.setViewsCount(0, 8 - currentRecycledViews) { fakeParent, viewType ->
                onCreateViewHolder(fakeParent, viewType)
            }
        }
    }

    private fun toggleView(vh: ImageItemViewHolder, toShow: Boolean? = null): Boolean {
        val itemNum = vh.itemNum
        val isClicked = toShow ?: !selectedArray.get(itemNum, false)
        selectedArray.put(itemNum, isClicked)
        if (isClicked) {
            vh.itemView.setBackgroundColor(
                    getColor(vh.itemView.context, R.color.itemOnClickOutSideColor)
            )
        } else {
            vh.itemView.background = null
            vh.innerRecyclerView.visibility = View.GONE
        }

        return isClicked
    }

    override fun onLoadFailed(itemNum: Int, isVisible: Boolean) {
        contentProvider.deleteImageItem(itemNum)?.let {
            if (isVisible) {
                notifyItemRemoved(it)
            }
        }
    }

    private fun showOtherImages(vh: ImageItemViewHolder) {
        if (subImagesProvider.getSubImagesCount(vh.itemNum) > 0) {
            vh.innerRecyclerView.visibility = View.VISIBLE
        } else {
            toggleView(vh)
            shortToast(R.string.other_images_not_found)
        }
    }

    fun onReloadData() {
        selectedArray.clear()
        notifyDataSetChanged()
    }

    fun onChangeScreenConfiguration(maxImageWidth: Int) {
        this.maxImageWidth = maxImageWidth
    }

    override fun getItemCount() = contentProvider.getItemCount()

    override fun onBindViewHolder(holder: ImageItemViewHolder, positionVh: Int) {
        val item = contentProvider.getItemOnPosition(positionVh)
        holder.maxImageWidth = maxImageWidth
        holder.bind(item)
        holder.innerRecyclerView.adapter?.notifyDataSetChanged()

        val isExpanded = toggleView(holder, selectedArray.get(item.itemNum, false))
        if (isExpanded) {
            showOtherImages(holder)
        }
    }
}