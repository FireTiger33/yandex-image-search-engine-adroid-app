package com.stacktivity.yandeximagesearchengine.util.image

import android.nfc.FormatException
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.Downloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.srcRect
import java.net.ConnectException
import kotlin.math.abs

/**
 * Used for conversion [BitmapObserver] to [Downloader.Observer].
 * Bitmap creation takes place in a separate thread and returns result to main thread.
 */
open class BitmapFactory(
    val imageObserver: BitmapObserver,
) : Downloader.Observer {
    override fun onSuccess(buffer: ByteArray, url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Default) {
                BitmapUtils.getBitmap(buffer)
            }?.let {
                imageObserver.onBitmapResult(it)
            } ?: run {
                imageObserver.onException(FormatException("Is not an image: $url"))
            }
        }
    }

    override fun onError(url: String, e: Throwable?) {
        imageObserver.onException(e ?: ConnectException("Load failed: $url"))
    }
}

/**
 * Used for conversion [ImageObserver] to [Downloader.Observer].
 * Expends [BitmapFactory] for separating work with Gif images.
 *
 * @see BitmapFactory for more details
 */
class ImageFactory(imageObserver: ImageObserver) : BitmapFactory(imageObserver) {
    override fun onSuccess(buffer: ByteArray, url: String) {
        if (BitmapUtils.bufferIsAGif(buffer)) {
            imageObserver as ImageObserver
            GifDrawable(buffer).run {
                val size = this.srcRect
                imageObserver.onGifResult(this, size.width(), size.height())
            }
        } else {
            super.onSuccess(buffer, url)
        }
    }
}

/**
 * Used for conversion [BitmapObserver] to [Downloader.Observer].
 * Bitmap creation takes place in a separate thread and returns result to main thread.
 *
 * If the size of the resulting image differs by more than 10% from expected size, then to
 * [BitmapObserver.onException] passed [IllegalStateException]
 */
open class BitmapFactoryWithSizeValidation(
    imageObserver: BitmapObserver,
    private val expectedWidth: Int, private val expectedHeight: Int,
) : BitmapFactory(imageObserver) {
    override fun onSuccess(buffer: ByteArray, url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Default) {
                BitmapUtils.getBitmap(buffer)
            }?.let {
                if (correctSize(expectedWidth, expectedHeight, it.width, it.height)) {
                    imageObserver.onBitmapResult(it)
                } else {
                    imageObserver.onException(IllegalStateException("Image is invalid: $url," +
                        "expected: ${expectedWidth}x$expectedHeight, real: ${it.width}x${it.height}"))
                }
            } ?: run {
                imageObserver.onException(FormatException("Is not an image: $url"))
            }
        }
    }

    /**
     * Used to check the correctness of the received image.
     * In the case that the image is not available, the server may return a different image.
     *
     * Size is correct with a tolerance of 10% of expectedWidth
     */
    private fun correctSize(
        expectedWidth: Int, expectedHeight: Int,
        realWidth: Int, realHeight: Int
    ): Boolean {
        val permissibleError = expectedWidth * 0.1

        return (abs(expectedWidth - realWidth) <= permissibleError
            && abs(expectedHeight - realHeight) <= permissibleError)
    }
}

/**
 * Used for conversion [ImageObserver] to [Downloader.Observer].
 * Expends [BitmapFactory] for separating work with Gif images.
 *
 * @see BitmapFactoryWithSizeValidation for more details
 */
class ImageFactoryWithSizeValidation(
    imageObserver: ImageObserver,
    expectedWidth: Int, expectedHeight: Int
) : BitmapFactoryWithSizeValidation(imageObserver, expectedWidth, expectedHeight) {
    override fun onSuccess(buffer: ByteArray, url: String) {
        if (BitmapUtils.bufferIsAGif(buffer)) {
            imageObserver as ImageObserver
            GifDrawable(buffer).run {
                val size = this.srcRect
                imageObserver.onGifResult(this, size.width(), size.height())
            }
        } else {
            super.onSuccess(buffer, url)
        }
    }
}