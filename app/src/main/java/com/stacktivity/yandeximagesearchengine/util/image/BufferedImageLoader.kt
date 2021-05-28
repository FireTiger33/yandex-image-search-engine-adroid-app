package com.stacktivity.yandeximagesearchengine.util.image

/**
 * Image loader implementation of the [BufferedImageProvider] interface.
 * Used for downloading image and getting result in [ImageObserver]
 *
 * When image is uploaded again, result will be returned from device cache
 */
object BufferedImageLoader : BufferedImageProvider<String> {
    val tag: String = BufferedImageLoader::class.java.simpleName
    override fun getImage(
        item: String,
        imageObserver: ImageObserver,
        previewImageObserver: BitmapObserver?
    ) = BufferedSmartImageDownloader(item, tag, imageObserver).download()

    override fun getCacheFile(item: String) = BufferedSmartBitmapDownloader.getCacheFile(item)

    fun removeAllTasks() {
        BufferedSmartBitmapDownloader.removeAllTasksByPoolTag(tag)
    }
}