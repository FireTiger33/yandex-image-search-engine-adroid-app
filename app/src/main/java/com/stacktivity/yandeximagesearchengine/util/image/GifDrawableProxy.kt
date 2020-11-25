@file:Suppress("PackageDirectoryMismatch")

package pl.droidsonroids.gif

import android.graphics.Rect

internal val GifDrawable.srcRect: Rect
    get() = Rect(0, 0, this.mNativeInfoHandle.width, this.mNativeInfoHandle.height)

