package com.stacktivity.yandeximagesearchengine.ui.main

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.webkit.URLUtil
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stacktivity.yandeximagesearchengine.util.prefetcher.PrefetchRecycledViewPool
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.ui.dialog.UserImageProviderDialog
import com.stacktivity.yandeximagesearchengine.ui.settings.SettingsActivity
import com.stacktivity.yandeximagesearchengine.ui.captcha.CaptchaDialog
import com.stacktivity.yandeximagesearchengine.util.shortToast
import com.stacktivity.yandeximagesearchengine.util.BitmapUtils
import com.stacktivity.yandeximagesearchengine.util.CacheWorker
import com.stacktivity.yandeximagesearchengine.util.Constants
import com.stacktivity.yandeximagesearchengine.util.ToolbarDemonstrator
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.*


private const val KEY_QUERY = "query"

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    private val viewModel: MainViewModel = MainViewModel.getInstance()
    private lateinit var searchView: SearchView
    private var showedMenu: PopupMenu? = null

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
        private var viewPool: PrefetchRecycledViewPool? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        setSupportActionBar(searchToolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        // checking open with intent
        val imageUri = intent.data ?: intent.extras?.get(Intent.EXTRA_STREAM) as Uri?
        imageUri?.let { fetchImagesByImageUri(it) }

        val showKeyboard = if (imageUri != null) false else savedInstanceState == null
        initUI(savedInstanceState, showKeyboard)
    }

    override fun onStop() {
        showedMenu?.dismiss()
        super.onStop()
    }

    private fun initUI(savedInstanceState: Bundle?, showKeyboard: Boolean) {
        setupSearchByImage()
        setupSearchView(
            setFocus = showKeyboard,
            savedQuery = savedInstanceState?.getString(KEY_QUERY, "")
        )
        setupImageList(savedInstanceState)
        setupObservers()
    }

    private fun setupSearchView(setFocus: Boolean, savedQuery: String?) {
        searchView = searchToolBar.findViewById(R.id.search)
        searchView.run {
            setQuery(savedQuery, false)
            setOnQueryTextListener(this@MainActivity)
            isFocusable = true
            isIconified = false
            if (setFocus) {
                requestFocusFromTouch()
            } else {
                clearFocus()
            }
        }
    }

    private fun setupSearchByImage() {
        btn_search_by_image.setOnClickListener {
            val requestKey = "imageUri"
            UserImageProviderDialog().show(supportFragmentManager, requestKey)
            supportFragmentManager.setFragmentResultListener(requestKey, this) { _, result ->
                val imagePfd = result.getParcelable<ParcelFileDescriptor>("pfd")
                BitmapUtils.getSimplifiedBitmap(imagePfd!!.fileDescriptor, 200, 200) { bitmap ->
                    bitmap?.let { fetchImagesByBitmap(it) }
                }
            }
        }
    }

    private fun setupImageList(savedInstanceState: Bundle?) {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val maxImageWidth = size.x

        setupViewPool()

        val layoutManager = image_list_rv.layoutManager as LinearLayoutManager
        layoutManager.recycleChildrenOnDetach = true
        image_list_rv.adapter = viewModel.getImageItemListAdapter(maxImageWidth).apply {
            prefetchViewHolders(viewPool!!)
        }

        image_list_rv.addOnScrollListener(getImageScrollListener(layoutManager))
    }

    private suspend fun fetchImagesByBitmap(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val compressedImageFile = CacheWorker.getTempFile()
        bitmap.compress(Bitmap.CompressFormat.PNG, 85, compressedImageFile.outputStream())
        withContext(Dispatchers.Main) {
            viewModel.fetchImagesByImage(compressedImageFile.path)
        }
    }

    private fun fetchImagesByImageUri(image: Uri) {
        val pfd = contentResolver.openFileDescriptor(image, "r")
        if (pfd == null) {
            Log.e(TAG, "Could not read selected image: $image")
            shortToast(R.string.selected_image_could_not_read)
            return
        }

        BitmapUtils.getSimplifiedBitmap(pfd.fileDescriptor, 200, 200) { bitmap ->
            bitmap?.let { fetchImagesByBitmap(it) }
        }
    }

    private fun setupViewPool() {
        if (viewPool == null) {
            viewPool = PrefetchRecycledViewPool(this).apply {
                prepare()
            }
        }
        image_list_rv.setRecycledViewPool(viewPool)
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

        viewModel.captchaEvent.observe(this, {
            it.getContentIfNotHandled()?.let { imageUrl ->
                val dialog = CaptchaDialog.newInstance(
                    imageUrl = imageUrl,
                    showFailedMsg = it.isRepeatEvent
                ) { captchaValue ->
                    it.setResult(captchaValue)
                }

                dialog.show(supportFragmentManager.beginTransaction(), CaptchaDialog.TAG)
            }
        })

        viewModel.showedMenu.observe(this, {  // TODO
            showedMenu = it
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
            searchView.clearFocus()
            if (URLUtil.isHttpUrl(query) || URLUtil.isHttpsUrl(query)) {
                viewModel.fetchImagesByImageUrl(query)
            } else {
                viewModel.fetchImagesOnQuery(query)
            }
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
        return when (item.itemId) {
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

    override fun onNightModeChanged(mode: Int) {
        viewPool?.clear()
        viewPool = null
    }
}