package com.stacktivity.yandeximagesearchengine.util

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.stacktivity.yandeximagesearchengine.App

fun getColor(context: Context, @ColorRes colorId: Int): Int {
    return context.resources.getColor(colorId, context.theme)
}

fun getString(@StringRes stringIdRes: Int): String {
    return App.getInstance().getString(stringIdRes)
}
