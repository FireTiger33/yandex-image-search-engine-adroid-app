package com.stacktivity.yandeximagesearchengine.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.ImageItemViewHolder
import java.io.File

class ImageListAdapter(
    private val contentProvider: ContentProvider,
    private val imageBufferFilesDir: File,
    private val eventListener: ImageItemViewHolder.EventListener,
    private val maxImageWidth: Int,
    private val defaultColor: Int
) : RecyclerView.Adapter<ImageItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_image_list, parent, false)
        return ImageItemViewHolder(view, eventListener, maxImageWidth, defaultColor)
    }

    override fun getItemCount() = contentProvider.getItemCount()

    override fun onBindViewHolder(holder: ImageItemViewHolder, position: Int) {
        val bufferFile = File(imageBufferFilesDir.path + File.separator + position)
        holder.bind(contentProvider.getItemOnPosition(position), bufferFile)
    }

    interface ContentProvider {
        fun getItemCount(): Int
        fun getItemOnPosition(position: Int): SerpItem
    }
}