package com.stacktivity.yandeximagesearchengine.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.ui.fragments.SettingsFragment

class SettingsActivity : AppCompatActivity() {

    companion object {
        val tag = SettingsActivity::class.java.simpleName
        fun start(context: Context) = context.startActivity(Intent(context, SettingsActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.container)
        supportActionBar?.title = getString(R.string.title_settings)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SettingsFragment())
            .commitNow()
    }
}