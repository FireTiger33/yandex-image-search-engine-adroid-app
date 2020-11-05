package com.stacktivity.yandeximagesearchengine.ui.adapter

import android.content.Intent
import android.net.Uri
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.providers.ImageItemsProvider
import com.stacktivity.yandeximagesearchengine.providers.SubImagesProvider
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.ImageItemViewHolder
import com.stacktivity.yandeximagesearchengine.util.getColor
import com.stacktivity.yandeximagesearchengine.util.getString
import com.stacktivity.yandeximagesearchengine.util.shortToast
import java.io.File

internal class ImageListAdapter(
    private val contentProvider: ImageItemsProvider,
    private val subImagesProvider: SubImagesProvider,
    private val imageBufferFilesDirPath: String,
    private var maxImageWidth: Int
) : RecyclerView.Adapter<ImageItemViewHolder>(), ImageItemViewHolder.EventListener {

    private var parentWidth: Int = 0
    private val selectedArray: SparseBooleanArray = SparseBooleanArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageItemViewHolder {
        parentWidth = parent.measuredWidth
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_image_list, parent, false)
        val vh = ImageItemViewHolder(view, this)
        vh.innerRecyclerView.run {
            adapter = SimpleImageListAdapter().apply {
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

                    override fun onItemClick(item: String) {
                        startActivity(
                            vh.itemView.context,
                            Intent(Intent.ACTION_VIEW).setData(Uri.parse(item)),
                            null
                        )
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
        val cacheDir = imageBufferFilesDirPath + File.separator
        val item = contentProvider.getItemOnPosition(positionVh)
        holder.maxImageWidth = maxImageWidth
        holder.bind(item, File(cacheDir + item.itemNum))
        (holder.innerRecyclerView.adapter as SimpleImageListAdapter?)?.apply {
            this.cacheDir = cacheDir
            notifyDataSetChanged()
        }
        toggleView(holder, selectedArray.get(item.itemNum, false))

        if (selectedArray.get(item.itemNum, false)) {
            showOtherImages(holder)
        }
    }
}