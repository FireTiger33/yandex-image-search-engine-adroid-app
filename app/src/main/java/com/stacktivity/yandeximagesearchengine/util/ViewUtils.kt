package com.stacktivity.yandeximagesearchengine.util

object ViewUtils {
    fun calculateViewWidth(viewHeight: Int, contentWidth: Int, contentHeight: Int): Int {
        val cropFactor: Double = viewHeight / contentHeight.toDouble()
        return (cropFactor * contentWidth).toInt()
    }

    fun calculateViewHeight(viewWidth: Int, contentWidth: Int, contentHeight: Int): Int {
        return calculateViewWidth(viewWidth, contentHeight, contentWidth)
    }
}