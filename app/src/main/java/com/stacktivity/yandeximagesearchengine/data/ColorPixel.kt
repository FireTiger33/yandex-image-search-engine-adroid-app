package com.stacktivity.yandeximagesearchengine.data

import android.graphics.Color
import androidx.annotation.FloatRange

data class ColorPixel(
    var r: Int,
    var g: Int,
    var b: Int
) {
    infix operator fun plus(pixel: ColorPixel) = ColorPixel(r + pixel.r, g + pixel.g, b + pixel.b)

    infix operator fun div(d: Int) = ColorPixel(r / d, g / d, b / d)

    infix operator fun times(factor: Float) = ColorPixel(
        (this.r * factor).toInt(),
        (this.g * factor).toInt(),
        (this.g * factor).toInt()
    )

    infix fun euclidean(p: ColorPixel): Double = kotlin.math.sqrt((
        (r - p.r) * (r - p.r)
            + (g - p.g) * (g - p.g)
            + (b - p.b) * (b - p.b)
        ).toDouble())

    fun toInt() = Color.rgb(r, g, b)

    /**
     * Darkens a color by a given factor.
     *
     * @param color to darken
     * @param factor to darken the color.
     * @return darker version of specified color.
     */
    fun dark(@FloatRange(from = 0.0, to = 1.0) factor: Float): ColorPixel {
        return this * factor
    }

    companion object {
        fun from(color: Int): ColorPixel {
            val r = (color shr 16).toInt() and 0xff
            val g = (color shr 8).toInt() and 0xff
            val b = color.toInt() and 0xff

            return ColorPixel(r, g, b)
        }
    }
}