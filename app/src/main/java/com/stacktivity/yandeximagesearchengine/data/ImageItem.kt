package com.stacktivity.yandeximagesearchengine.data

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
)