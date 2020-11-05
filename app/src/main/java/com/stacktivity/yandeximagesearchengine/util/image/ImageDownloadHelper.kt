package com.stacktivity.yandeximagesearchengine.util.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.stacktivity.yandeximagesearchengine.util.ByteArrayDownloader
import com.stacktivity.yandeximagesearchengine.util.NetworkStateReceiver
import java.nio.ByteBuffer

class ImageDownloadHelper private constructor() {

    companion object {
        val tag: String = ImageDownloadHelper::class.java.simpleName
        var maxQueueCount: Int = 3  // number of images loading simultaneously

        private var INSTANCE: ImageDownloadHelper? = null
        fun getInstance() = INSTANCE
            ?: ImageDownloadHelper().also {
                INSTANCE = it
            }
    }

    abstract class ImageObserver {
        abstract fun onBitmapResult(bitmap: Bitmap?)
        abstract fun onGifResult(buffer: ByteBuffer)
    }

    fun getOneOfImage(
        poolTag: String,
        urls: List<String>,
        imageObserver: ImageObserver,
        reqWidth: Int? = null, reqHeight: Int? = null,
        minWidth: Int? = null, minHeight: Int? = null,
        timeoutMs: Long? = null
    ) {
        val listener = object : Runnable {
            private var i = 0
            private val localObserver = object: ImageObserver() {
                override fun onBitmapResult(bitmap: Bitmap?) {
                    if (bitmap != null
                            && bitmap.width >= minWidth?: bitmap.width
                            && bitmap.height >= minHeight?: bitmap.height) {
                        imageObserver.onBitmapResult(bitmap)
                    }
                    else {
                        if (++i < urls.size) {
                            run()
                        } else {
                            imageObserver.onBitmapResult(null)
                        }
                    }
                }

                override fun onGifResult(buffer: ByteBuffer) {
                    imageObserver.onGifResult(buffer)
                    i++
                }
            }

            override fun run() {
                getImageAsync(poolTag, urls[i], reqWidth, reqHeight, timeoutMs, localObserver)
            }
        }

        listener.run()
    }

    /**
     * Download image
     *
     * If only one of required resolution parameters is set, image will change proportionally
     *
     * @param poolTag   images with different pool tags are loaded in parallel
     * @param url       direct link to image
     * @param timeoutMs max time to wait for connection to image server in milliseconds
     * @param reqWidth  required width
     * @param reqHeight required height
     *
     * @return image Bitmap or null if download failed
     */
    fun getBitmapAsync(
        poolTag: String,
        url: String,
        reqWidth: Int? = null, reqHeight: Int? = null,
        timeoutMs: Long? = null,
        onResult: (bitmap: Bitmap?) -> Unit
    ) {
        checkPool(poolTag)

        NetworkStateReceiver.getInstance().addListener(
            poolTag,
            ByteArrayDownloader(url, timeoutMs) { buffer ->
                onResult(getBitmap(buffer, reqWidth, reqHeight))
            }
        )
    }

    fun getImageAsync(
        poolTag: String,
        url: String,
        reqWidth: Int? = null, reqHeight: Int? = null,
        timeoutMs: Long? = null,
        imageObserver: ImageObserver
    ) {
        checkPool(poolTag)

        NetworkStateReceiver.getInstance().addListener(
            poolTag,
            ByteArrayDownloader(url, timeoutMs) { buffer ->
                buffer?.rewind()
                if (buffer != null && buffer.capacity() > 3) {
                    val head = ByteArray(3)
                    buffer.get(head, 0, 3)
                    buffer.rewind()
                    if (head.toString(Charsets.UTF_8) == "GIF") {
                        imageObserver.onGifResult(buffer)
                    } else {
                        imageObserver.onBitmapResult(getBitmap(buffer, reqWidth, reqHeight))
                    }
                } else {
                    imageObserver.onBitmapResult(null)
                }
            }
        )
    }

    private fun checkPool(poolTag: String) {
        if (NetworkStateReceiver.getInstance().getListenersCountByTag(poolTag) == null) {
            NetworkStateReceiver.getInstance().addNewPoolListeners(poolTag, maxQueueCount)
        }
    }

    private fun getBitmap(buffer: ByteBuffer?, reqWidth: Int?, reqHeight: Int?): Bitmap? {  // TODO return cropped and original bitmap
        buffer?.rewind()
        val byteArray = ByteArray(buffer?.remaining()?: 0)
        buffer?.get(byteArray)
        val preResult: Bitmap? = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        return if (preResult != null && (reqWidth != null || reqHeight != null)) {
            // Calc out image size
            val mReqWidth: Int = reqWidth
                ?: (reqHeight!!.toFloat() / preResult.height * preResult.width).toInt()
            val mReqHeight: Int = reqHeight
                ?: (reqWidth!!.toFloat() / preResult.width * preResult.height).toInt()

            if (mReqWidth >= preResult.width) {
                preResult
            } else {
                Bitmap.createScaledBitmap(preResult, mReqWidth, mReqHeight, false)
            }
        } else {
            preResult
        }
    }
}
