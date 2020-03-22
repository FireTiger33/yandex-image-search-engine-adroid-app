package com.stacktivity.yandeximagesearchengine

import android.app.Application
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
            if (sp.getBoolean("darkTheme", false)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        } else {
            val nightMode = AppCompatDelegate.getDefaultNightMode()
            val resValue = nightMode == AppCompatDelegate.MODE_NIGHT_YES
            sp.edit().putBoolean("darkTheme", resValue).apply()

            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

}