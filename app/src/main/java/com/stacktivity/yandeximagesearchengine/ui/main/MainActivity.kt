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
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.ui.SettingsActivity
import com.stacktivity.yandeximagesearchengine.ui.captcha.CaptchaDialog
import com.stacktivity.yandeximagesearchengine.util.Constants
import com.stacktivity.yandeximagesearchengine.util.ToolbarDemonstrator
import com.stacktivity.yandeximagesearchengine.util.hideKeyboard
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private val viewModel: MainViewModel = MainViewModel.getInstance()
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(searchToolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        initUI()
    }

    private fun setupImageList() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val maxImageWidth = (size.x * 0.8).toInt()
        val layoutManager = image_list_rv.layoutManager as LinearLayoutManager
        image_list_rv.adapter = viewModel.getImageItemListAdapter(maxImageWidth)
        image_list_rv.addOnScrollListener(getImageScrollListener(layoutManager))
    }

    private fun setupObservers() {
        viewModel.dataLoading.observe(this, Observer {
            progress_bar.visibility =
                if (it /*&& viewModel.empty.value != false*/) View.VISIBLE
                else View.GONE
        })

        viewModel.newQueryIsLoaded.observe(this, Observer {
            if (it && image_list_rv.childCount > 0) {
                image_list_rv.scrollToPosition(0)
            }
        })

        viewModel.onImageClickEvent.observe(this, Observer {
            it.getContentIfNotHandled()?.let { imageUrl ->
                startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(imageUrl))
                )
            }
        })

        viewModel.captchaEvent.observe(this, Observer {
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

                // Show / hide SearchToolbar
                val currentFirstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                if (currentFirstVisibleItem > this.mLastFirstVisibleItem) {
                    ToolbarDemonstrator.hideActionBar(searchToolBar, supportActionBar!!, 200)
                } else if (currentFirstVisibleItem < this.mLastFirstVisibleItem) {
                    ToolbarDemonstrator.showActionBar(searchToolBar, supportActionBar!!, 200)
                }

                this.mLastFirstVisibleItem = currentFirstVisibleItem


                // Request to load a new batch of images when 40% of the current batch is reached
                val itemCount = layoutManager.itemCount
                val lastVisibleItemPosition: Int = layoutManager.findLastVisibleItemPosition()

                if (itemCount - lastVisibleItemPosition <= Constants.PAGE_SIZE * 0.4) {
                    viewModel.fetchImagesOnNextPage()
                }

                progress_bar.visibility =
                    if (viewModel.dataLoading.value != false && lastVisibleItemPosition + 1 == layoutManager.itemCount) View.VISIBLE
                    else View.GONE
            }
        }
    }

    private fun initUI() {
        searchView = searchToolBar.findViewById(R.id.search)
        searchView.setOnQueryTextListener(this)
        setupImageList()
        setupObservers()
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
        when(item.itemId) {
            R.id.settings -> {
                SettingsActivity.start(this)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }
}