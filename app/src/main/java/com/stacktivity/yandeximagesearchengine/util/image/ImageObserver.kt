package com.stacktivity.yandeximagesearchengine.util.image

import android.graphics.Bitmap
import pl.droidsonroids.gif.GifDrawable

abstract class BitmapObserver {
    abstract fun onBitmapResult(bitmap: Bitmap)
    abstract fun onException(e: Throwable)
}

abstract class ImageObserver : BitmapObserver() {
    abstract fun onGifResult(drawable: GifDrawable, width: Int, height: Int)
}