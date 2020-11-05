package com.stacktivity.yandeximagesearchengine.ui.main

import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.ui.SettingsActivity
import com.stacktivity.yandeximagesearchengine.ui.captcha.CaptchaDialog
import com.stacktivity.yandeximagesearchengine.util.Constants
import com.stacktivity.yandeximagesearchengine.util.ToolbarDemonstrator
import com.stacktivity.yandeximagesearchengine.util.hideKeyboard
import kotlinx.android.synthetic.main.main_activity.*

const val KEY_QUERY = "query"

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    private val viewModel: MainViewModel = MainViewModel.getInstance()
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(searchToolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        initUI(savedInstanceState)
    }

    private fun initUI(savedInstanceState: Bundle?) {
        val savedQuery = savedInstanceState?.getString(KEY_QUERY, "")
        setupSearchView(
            requestFocus = savedInstanceState == null,
            savedQuery = savedQuery)
        setupImageList()
        setupObservers()
    }

    private fun setupSearchView(requestFocus: Boolean, savedQuery: String?) {
        searchView = searchToolBar.findViewById(R.id.search)
        searchView.run {
            setQuery(savedQuery, false)
            setOnQueryTextListener(this@MainActivity)
            isFocusable = true
            isIconified = false
            if (requestFocus) {
                requestFocusFromTouch()
            }
        }
    }

    private fun setupImageList() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val maxImageWidth = size.x
        val layoutManager = image_list_rv.layoutManager as LinearLayoutManager
        image_list_rv.adapter = viewModel.getImageItemListAdapter(maxImageWidth)
        image_list_rv.addOnScrollListener(getImageScrollListener(layoutManager))
    }

    private fun setupObservers() {
        viewModel.dataLoading.observe(this, {
            progress_bar.visibility =
                if (it) View.VISIBLE
                else View.GONE
        })

        viewModel.newQueryIsLoaded.observe(this, {
            if (it && image_list_rv.childCount > 0) {
                image_list_rv.scrollToPosition(0)
            }
        })

        viewModel.onImageClickEvent.observe(this, {
            it.getContentIfNotHandled()?.let { imageUrl ->
                startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(imageUrl))
                )
            }
        })

        viewModel.captchaEvent.observe(this, {
            it.getContentIfNotHandled()?.let { imageUrl ->
                val dialog = CaptchaDialog(
                    imageUrl = imageUrl,
                    showFailedMsg = it.isRepeatEvent
                ) { captchaValue ->
                    it.setResult(captchaValue)
                }

                dialog.show(supportFragmentManager.beginTransaction(), CaptchaDialog.tag)
            }
        })
    }

    private fun getImageScrollListener(layoutManager: LinearLayoutManager): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            var mLastFirstVisibleItem = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val currentFirstVisibleItem = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition: Int = layoutManager.findLastVisibleItemPosition()
                val itemCount = layoutManager.itemCount

                // Show / hide SearchToolbar
                if (currentFirstVisibleItem > this.mLastFirstVisibleItem) {
                    ToolbarDemonstrator.hideActionBar(searchToolBar, supportActionBar!!, 200)
                } else if (currentFirstVisibleItem < this.mLastFirstVisibleItem) {
                    ToolbarDemonstrator.showActionBar(searchToolBar, supportActionBar!!, 200)
                }
                this.mLastFirstVisibleItem = currentFirstVisibleItem

                // Request to load a new batch of images when 40% of the current batch is reached
                if (itemCount - lastVisibleItemPosition <= Constants.PAGE_SIZE * 0.4) {
                    viewModel.fetchImagesOnNextPage()
                }

                progress_bar.visibility =
                    if (viewModel.dataLoading.value != false && lastVisibleItemPosition + 1 == layoutManager.itemCount) View.VISIBLE
                    else View.GONE
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {
            hideKeyboard(this, searchView)
            searchView.clearFocus()
            viewModel.fetchImagesOnQuery(query)
        }

        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.settings -> {
                SettingsActivity.start(this)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_QUERY, searchView.query.toString())
        super.onSaveInstanceState(outState)
    }
}