package com.stacktivity.yandeximagesearchengine.data

/**
* Main class used to store search result element
*/
data class ImageItem(
    val title: String,
    var sourceSite: String,
    val dups: List<ImageData>
)

data class ImageData(
    val width: Int,
    val height: Int,
    val fileSizeInBytes: Int,
    val url: String
)