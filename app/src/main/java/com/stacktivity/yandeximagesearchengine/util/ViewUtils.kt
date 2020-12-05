package com.stacktivity.yandeximagesearchengine.util

import android.text.TextPaint
import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.TextView

object ViewUtils {
    fun calculateViewWidth(viewHeight: Int, contentWidth: Int, contentHeight: Int): Int {
        val cropFactor: Double = viewHeight / contentHeight.toDouble()
        return (cropFactor * contentWidth).toInt()
    }

    fun calculateViewHeight(viewWidth: Int, contentWidth: Int, contentHeight: Int): Int {
        return calculateViewWidth(viewWidth, contentHeight, contentWidth)
    }


    /**
     * Recursive calculating max size of [view.text] so that it fits in a single line
     *
     * @param view          - your [TextView] with required text
     * @param targetWidth   - target width with possible horizontal margins
     * @param low           - min text size
     * @param high          - max text size
     * @param precision     - precision of calculations (default is scaled density)
     */
    fun calculateOptimalTextSizeForSingleLine(
        view: TextView,
        targetWidth: Int,
        low: Float, high: Float,
        precision: Float = view.resources.displayMetrics.scaledDensity
    ): Float {
        val textPaint = TextPaint(view.paint).apply { textSize = view.textSize }
        return calculateOptimalTextSizeForSingleLine(
            view.text, textPaint, targetWidth - precision.toInt(),
            low, high,
            precision,
            view.resources.displayMetrics
        )
    }


    /**
     * Recursive calculating max size of [text] so that it fits in a single line
     *
     * @param text          - text to fit in one line
     * @param paint         - copy paint of your view
     * @param targetWidth
     * @param low           - min text size
     * @param high          - max text size
     * @param precision     - precision of calculations
     * @param displayMetrics
     */
    fun calculateOptimalTextSizeForSingleLine(
        text: CharSequence, paint: TextPaint,
        targetWidth: Int, low: Float, high: Float, precision: Float,
        displayMetrics: DisplayMetrics
    ): Float {
        if (getTextWidth(text, paint, high, displayMetrics) <= targetWidth)
            return high

        val mid = (low + high) / 2.0f
        val maxLineWidth = getTextWidth(text, paint, mid, displayMetrics)

        return when {
            (high - low) < precision -> low

            maxLineWidth > targetWidth -> {
                calculateOptimalTextSizeForSingleLine(
                    text, paint, targetWidth, low, mid, precision,
                    displayMetrics
                )
            }

            maxLineWidth < targetWidth -> {
                calculateOptimalTextSizeForSingleLine(
                    text, paint, targetWidth, mid, high, precision,
                    displayMetrics
                )
            }

            else -> mid
        }
    }

    private fun getTextWidth(text: CharSequence, paint: TextPaint, textSizePx: Float, displayMetrics: DisplayMetrics): Float {
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, textSizePx, displayMetrics)
        return paint.measureText(text, 0, text.length)
    }
}