package com.stacktivity.yandeximagesearchengine.util.image

import android.graphics.Bitmap
import pl.droidsonroids.gif.GifDrawable

interface BitmapObserver {
    fun onBitmapResult(bitmap: Bitmap) {}
    fun onException(e: Throwable) {}
}

interface ImageObserver : BitmapObserver {
    fun onGifResult(drawable: GifDrawable, width: Int, height: Int) {}
}