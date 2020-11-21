package com.stacktivity.yandeximagesearchengine

import android.app.Application
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources.Theme
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.stacktivity.yandeximagesearchengine.util.NetworkStateReceiver

class App: Application() {

    companion object {
        private lateinit var instance: App

        @Synchronized
        fun getInstance(): App = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        NetworkStateReceiver.register(applicationContext)

        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (sp.contains("darkTheme")) {
            toggleTheme(sp.getBoolean("darkTheme", false))
        } else {
            val nightMode = theme.nightModeIsActive()
            sp.edit().putBoolean("darkTheme", nightMode).apply()
            toggleTheme(nightMode)
        }
    }

    private fun toggleTheme(nightMode: Boolean) {
        if (nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

}

fun Theme.nightModeIsActive(): Boolean {
    return resources.configuration.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
}