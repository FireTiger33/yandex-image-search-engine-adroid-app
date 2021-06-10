package com.stacktivity.yandeximagesearchengine

import android.app.Application
import com.stacktivity.yandeximagesearchengine.util.NetworkStateReceiver
import com.stacktivity.yandeximagesearchengine.util.ThemeUtils

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

        configurationSettings()
    }

    private fun configurationSettings() {
        ThemeUtils.applyTheme()
    }

}