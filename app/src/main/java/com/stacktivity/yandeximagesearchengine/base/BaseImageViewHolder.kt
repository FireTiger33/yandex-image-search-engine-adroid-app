package com.stacktivity.yandeximagesearchengine.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.nio.ByteBuffer

internal abstract class BaseImageViewHolder(itemView: View): ViewHolder(itemView) {

    protected fun calculateViewWidth(viewHeight: Int, imageWidth: Int, imageHeight: Int): Int {
        val cropFactor: Double = viewHeight / imageHeight.toDouble()
        return (cropFactor * imageWidth).toInt()
    }

    protected fun calculateViewHeight(viewWidth: Int, imageWidth: Int, imageHeight: Int): Int {
        return calculateViewWidth(viewWidth, imageHeight, imageWidth)
    }

}