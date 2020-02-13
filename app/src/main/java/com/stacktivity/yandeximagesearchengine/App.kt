package com.stacktivity.yandeximagesearchengine

import android.app.Application

class App: Application() {

    companion object {
        private lateinit var instance: App

        @Synchronized
        fun getInstance(): App = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}