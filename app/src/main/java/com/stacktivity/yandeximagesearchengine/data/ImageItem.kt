package com.stacktivity.yandeximagesearchengine.data

import android.os.Parcel
import android.os.Parcelable
import com.stacktivity.yandeximagesearchengine.data.model.Thumb

/**
 * Main class used to store search result element
 */
data class ImageItem(
    val itemNum: Int,
    val title: String,
    var sourceSite: String,
    val dups: MutableList<ImageData>,
    val thumb: Thumb,
    var colorSpace: List<ColorPixel>? = null
)

data class ImageData(
    val width: Int,
    val height: Int,
    val fileSizeInBytes: Int,
    val url: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: ""
    )

    fun baseToString(): String {
        return String.format("%dx%d (%.2f Kb)",
            width, height,
            fileSizeInBytes.toFloat() / 1024
        )
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(width)
        parcel.writeInt(height)
        parcel.writeInt(fileSizeInBytes)
        parcel.writeString(url)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ImageData> {
        override fun createFromParcel(parcel: Parcel): ImageData {
            return ImageData(parcel)
        }

        override fun newArray(size: Int): Array<ImageData?> {
            return arrayOfNulls(size)
        }
    }
}