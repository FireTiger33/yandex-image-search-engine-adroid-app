package com.stacktivity.yandeximagesearchengine.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem

class ImageListAdapter(private val contentProvider: ContentProvider): RecyclerView.Adapter<ImageListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View
    ): RecyclerView.ViewHolder(itemView) {

        fun bind(item: SerpItem) {
            val weight = item.preview[0].w
            val height = item.preview[0].h
            val imageResolutionText = "original size: ${weight}x$height"
            val imageSizeText = "size: ${item.preview[0].fileSizeInBytes / 1024}Kb"

            itemView.findViewById<TextView>(R.id.title).text = item.snippet.title
            itemView.findViewById<TextView>(R.id.image_resolution).text = imageResolutionText
            itemView.findViewById<TextView>(R.id.image_size).text = imageSizeText
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_image_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = contentProvider.getItemCount()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(contentProvider.getItemOnPosition(position))
    }

    interface ContentProvider {
        fun getItemCount(): Int
        fun getItemOnPosition(position: Int): SerpItem
    }
}