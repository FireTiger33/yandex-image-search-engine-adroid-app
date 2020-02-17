package com.stacktivity.yandeximagesearchengine

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.SearchView
import com.stacktivity.yandeximagesearchengine.ui.main.MainFragment
import com.stacktivity.yandeximagesearchengine.util.hideKeyboard
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private val mainFragment = MainFragment.getInstance()
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(searchToolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        initUI()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, mainFragment)
                    .commitNow()
        }
    }

    private fun initUI() {
        searchView = searchToolBar.findViewById(R.id.search)
        searchView.setOnQueryTextListener(this)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {
            hideKeyboard(this, searchView)
            searchView.clearFocus()
            mainFragment.showSearchResult(query)
        }

        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }
}