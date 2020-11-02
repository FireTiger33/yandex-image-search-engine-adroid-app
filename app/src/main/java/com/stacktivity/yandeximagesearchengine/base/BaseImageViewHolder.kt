package com.stacktivity.yandeximagesearchengine.base

import android.graphics.BitmapFactory
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.FileWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
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

    protected fun getImageSize(file: File): Pair<Int, Int> {
        if (BitmapUtils.fileIsAnImage(file.path)) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, options)

            return Pair(options.outWidth, options.outHeight)
        } else {
            throw IllegalArgumentException("File is not an image")
        }
    }
}