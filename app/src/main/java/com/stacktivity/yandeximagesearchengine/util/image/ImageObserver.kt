package com.stacktivity.yandeximagesearchengine.util.image

import android.graphics.Bitmap
import pl.droidsonroids.gif.GifDrawable

abstract class ImageObserver {
    abstract fun onBitmapResult(bitmap: Bitmap)
    abstract fun onGifResult(drawable: GifDrawable, width: Int, height: Int)
    abstract fun onException(e: Throwable)
}