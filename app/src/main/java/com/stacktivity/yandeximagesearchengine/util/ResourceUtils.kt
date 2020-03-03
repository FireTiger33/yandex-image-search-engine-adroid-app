package com.stacktivity.yandeximagesearchengine.util

import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.stacktivity.yandeximagesearchengine.App

fun getColor(@ColorRes colorId: Int): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        App.getInstance().resources.getColor(colorId, App.getInstance().theme)
    } else {
        ContextCompat.getColor(App.getInstance(), colorId)
    }
}

fun getString(@StringRes stringIdRes: Int): String {
    return App.getInstance().getString(stringIdRes)
}