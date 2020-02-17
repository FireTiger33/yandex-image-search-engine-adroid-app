package com.stacktivity.yandeximagesearchengine.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.SimpleImageViewHolder
import java.io.File

class SimpleImageListAdapter(
    private val eventListener: EventListener,
    private val defaultColor: Int
) : RecyclerView.Adapter<SimpleImageViewHolder>(), SimpleImageViewHolder.EventListener {

    interface EventListener {
        fun onImagesLoadFailed()
    }

    var bufferFileBase: String? = null

    private var linkListToImages: ArrayList<String> = arrayListOf()

    fun setNewLinkListToImages(linkList: List<String>) {
        if (linkListToImages.isNotEmpty()) {
            clearImageList()
        }
        linkListToImages.addAll(linkList)
        notifyDataSetChanged()
    }

    fun clearImageList() {
        linkListToImages = arrayListOf()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleImageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.simple_item_image_list, parent, false)
        return SimpleImageViewHolder(view, this, defaultColor)
    }

    override fun getItemCount(): Int = linkListToImages.size

    override fun onBindViewHolder(holder: SimpleImageViewHolder, position: Int) {
        val bufferFile: File? = if (bufferFileBase != null) {
            File("${bufferFileBase}_$position")
        } else null
        holder.bind(linkListToImages[position], bufferFile)
    }

    override fun onImageLoadFailed(imageUrl: String) {
        Log.d("SimpleImageListAdapter", "load failed: $imageUrl")
        deleteImageFromList(imageUrl)
    }

    override fun onSmallImage(imageUrl: String) {
        Log.d("SimpleImageListAdapter", "image is too small: $imageUrl")
        deleteImageFromList(imageUrl)
    }

    private fun deleteImageFromList(imageUrl: String) {
        val deletedItemIndex = linkListToImages.indexOf(imageUrl)
        if (deletedItemIndex > -1) {
            linkListToImages.removeAt(deletedItemIndex)
            notifyItemRemoved(deletedItemIndex)
        }
        if (linkListToImages.isEmpty()) {
            eventListener.onImagesLoadFailed()
        }
    }
}