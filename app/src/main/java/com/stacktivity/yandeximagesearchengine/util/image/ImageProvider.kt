package com.stacktivity.yandeximagesearchengine.util.image

import java.io.File

interface ImageProvider<T> {
    fun getImage(
        item: T,
        imageObserver: ImageObserver,
        previewImageObserver: ImageObserver? = null,
    )
}

interface BufferedImageProvider<T>: ImageProvider<T> {
    fun getCacheFile(item: T): File
}