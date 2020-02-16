package com.stacktivity.yandeximagesearchengine.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.SimpleImageViewHolder

class SimpleImageListAdapter(
    private val eventListener: EventListener
) : RecyclerView.Adapter<SimpleImageViewHolder>(), SimpleImageViewHolder.EventListener {

    interface EventListener {
        fun onImagesLoadFailed()
    }

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
        return SimpleImageViewHolder(view, this)
    }

    override fun getItemCount(): Int = linkListToImages.size

    override fun onBindViewHolder(holder: SimpleImageViewHolder, position: Int) {
        holder.bind(linkListToImages[position])
    }

    override fun onImageLoadFailed(imageUrl: String) {
        Log.d("SimpleImageListAdapter", "load failed: $imageUrl")
        val deletedItemIndex = linkListToImages.indexOf(imageUrl)
        linkListToImages.removeAt(deletedItemIndex)
        notifyItemRemoved(deletedItemIndex)
        if (linkListToImages.isEmpty()) {
            eventListener.onImagesLoadFailed()
        }
    }
}