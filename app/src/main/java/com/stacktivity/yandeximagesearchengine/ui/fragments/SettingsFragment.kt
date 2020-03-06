package com.stacktivity.yandeximagesearchengine.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import com.stacktivity.yandeximagesearchengine.R.xml.preferences

class SettingsFragment : PreferenceFragmentCompat(),  SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(preferences)
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = preferenceScreen.sharedPreferences
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        val sharedPreferences = preferenceScreen.sharedPreferences
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "darkTheme") {
            val value: Boolean = sharedPreferences?.getBoolean(key, false)?: false
            if (value) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
}