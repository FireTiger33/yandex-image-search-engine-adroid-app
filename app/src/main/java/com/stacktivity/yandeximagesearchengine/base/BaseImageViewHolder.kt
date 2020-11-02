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

    @ExperimentalUnsignedTypes
    protected fun getGifSize(buffer: ByteBuffer): Pair<Int, Int> {
        val bArray = ByteArray(12)
        buffer.get(bArray, 0, 11)
        buffer.rewind()
        val width = bArray[6].toUByte() + (bArray[7].toUInt() shl 8)
        val height = bArray[8].toUByte() + (bArray[9].toUInt() shl 8)

        return Pair(width.toInt(), height.toInt())
    }
}