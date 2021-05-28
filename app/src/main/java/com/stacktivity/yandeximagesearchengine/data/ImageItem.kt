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


/**
 * @param width             - image width
 * @param height            - image height
 * @param fileSizeInBytes   - image size
 * @param url               - link to the image
 * @param loadState         - download status
 *
 * @see [LoadState]
 */
data class ImageData(
    val width: Int,
    val height: Int,
    val fileSizeInBytes: Int,
    val url: String,
    var loadState: LoadState = LoadState.None
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


/**
 * [None]         - state is not defined
 * [Loaded]       - indicates that file was downloaded successfully
 * [NotAvailable] - indicates that file can't be loaded because it was probably deleted,
 *                  access requires auth on corresponding resource
 *                  or their was timeout when connection
 * [Unreachable]  - indicates that an external connection via
 *                  Proxy or VPN is required to access this image
 * [Protected]    - protected from bots
 */
enum class LoadState {
    None,
    Loaded,
    NotAvailable,
    Unreachable,
    Protected
}