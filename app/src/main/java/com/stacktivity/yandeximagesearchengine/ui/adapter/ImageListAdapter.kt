package com.stacktivity.yandeximagesearchengine.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.ImageItemViewHolder

class ImageListAdapter(
    private val contentProvider: ContentProvider, private val maxImageWidth: Int
) : RecyclerView.Adapter<ImageItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_image_list, parent, false)
        return ImageItemViewHolder(view, maxImageWidth)
    }

    override fun getItemCount() = contentProvider.getItemCount()

    override fun onBindViewHolder(holder: ImageItemViewHolder, position: Int) {
        holder.bind(contentProvider.getItemOnPosition(position))
    }

    interface ContentProvider {
        fun getItemCount(): Int
        fun getItemOnPosition(position: Int): SerpItem
    }
}