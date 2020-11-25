package com.stacktivity.yandeximagesearchengine.util.image

import com.stacktivity.yandeximagesearchengine.util.Downloader

/**
 * Image loader implementation of the [BufferedImageProvider] interface.
 * Used for downloading image and getting result in [ImageObserver]
 *
 * @see Downloader for more details about the download procedure
 */
object ImageLoader : ImageProvider<String> {

    private val tag: String = ImageLoader::class.java.simpleName

    override fun getImage(
        item: String,
        imageObserver: ImageObserver,
        previewImageObserver: BitmapObserver?
    ) {
        val observer = ImageFactory(imageObserver)
        Downloader.downloadAsync(tag, item, observer)
    }

    fun getBitmap(item: String, imageObserver: BitmapObserver) {
        val observer = BitmapFactory(imageObserver)
        Downloader.downloadAsync(tag, item, observer)
    }
}