package com.stacktivity.yandeximagesearchengine.util

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Resources.Theme
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.stacktivity.yandeximagesearchengine.App
import com.stacktivity.yandeximagesearchengine.R


object ThemeUtils {

    val isDarkMode: Boolean
        get() {
            val context: Context = App.getInstance().applicationContext
            return isDarkMode(context)
        }

    fun toggleTheme(nightMode: Boolean) {
        if (nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun applyTheme() = toggleTheme(isDarkMode)

    fun getContextThemeWrapper(context: Context?): ContextThemeWrapper {
        return ContextThemeWrapper(context, R.style.MainTheme)
    }

    private fun isDarkMode(context: Context): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)

        return if (sp.contains("darkTheme")) {
            sp.getBoolean("darkTheme", false)
        } else {
            context.theme.nightModeIsActive().also {
                sp.edit().putBoolean("darkTheme", it).apply()
            }
        }
    }

    private fun Theme.nightModeIsActive(): Boolean {
        return resources.configuration.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    }
}