package com.stacktivity.yandeximagesearchengine.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.SimpleImageViewHolder
import com.stacktivity.yandeximagesearchengine.util.image.BufferedImageProvider

internal class SimpleImageListAdapter(private val imageLoader: BufferedImageProvider<String>)
    : RecyclerView.Adapter<SimpleImageViewHolder>(), SimpleImageViewHolder.EventListener
{

    lateinit var eventListener: EventListener

    interface EventListener {
        fun onImagesLoadFailed()
    }

    private var contentProvider: ContentProvider? = null


    fun setNewContentProvider(contentProvider: ContentProvider) {
        this.contentProvider = null
        notifyDataSetChanged()
        this.contentProvider = contentProvider
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleImageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.simple_item_image_list, parent, false)
        return SimpleImageViewHolder(view, this)
    }

    override fun getItemCount(): Int {
        if (contentProvider == null) {
            Log.e("SimpleImageListAdapter", "Need to set contentProvider using [SimpleImageListAdapter#setContentProvider]")
        }

        return contentProvider?.getItemCount() ?: 0
    }

    override fun onBindViewHolder(holder: SimpleImageViewHolder, position: Int) {
        val item = contentProvider!!.getItemOnPosition(position)

        holder.bind(item, imageLoader)
    }

    override fun onImageLoadFailed(imageUrl: String) {
        Log.i("SimpleImageListAdapter", "load failed: $imageUrl")
        deleteImageFromList(imageUrl)
    }

    override fun onSmallImage(imageUrl: String) {
        Log.i("SimpleImageListAdapter", "image is too small: $imageUrl")
        deleteImageFromList(imageUrl)
    }

    private fun deleteImageFromList(imageUrl: String) {
        val deletedItemIndex = contentProvider?.deleteItem(imageUrl)?: -1
        if (deletedItemIndex > -1) {
            notifyItemRemoved(deletedItemIndex)
        }
        if (contentProvider?.getItemCount()?: 0 < 1) {
            eventListener.onImagesLoadFailed()
        }
    }

    interface ContentProvider {
        fun getItemCount(): Int
        fun getItemOnPosition(position: Int): String
        fun deleteItem(imageUrl: String): Int
    }
}