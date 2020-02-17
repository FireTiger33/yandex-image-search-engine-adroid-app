package com.stacktivity.yandeximagesearchengine.ui.main

import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.stacktivity.yandeximagesearchengine.App
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.util.YandexImageUtil
import com.stacktivity.yandeximagesearchengine.base.BaseViewModel
import com.stacktivity.yandeximagesearchengine.data.model.ImageData
import com.stacktivity.yandeximagesearchengine.data.model.MainRepository
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem
import com.stacktivity.yandeximagesearchengine.ui.adapter.ImageListAdapter
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.ImageItemViewHolder
import com.stacktivity.yandeximagesearchengine.util.shortToast
import java.io.File

class MainViewModel : BaseViewModel() {
    val newQueryIsLoaded = MutableLiveData<Boolean>().apply { value = false }
    private var numLoadedPages: Int = 0
    private var currentQuery: String = ""
    private var isLastPage = false

    private val imageList: List<SerpItem>
        get() = MainRepository.getInstance().getImageList()
    private val imageCount: Int
        get() = MainRepository.getInstance().getImageCount()

    private val imageBufferFilesDir: File
        get() {
            return App.getInstance().filesDir
        }

    private var adapter: ImageListAdapter? = null

    fun getImageItemListAdapter(maxImageWidth: Int): ImageListAdapter = adapter
        ?: ImageListAdapter(
            object : ImageListAdapter.ContentProvider {
                override fun getItemCount(): Int = imageCount
                override fun getItemOnPosition(position: Int): SerpItem = imageList[position]
            },
            imageBufferFilesDir,
            object : ImageItemViewHolder.EventListener {
                override fun onImageLoadFailed(item: SerpItem) {
                    Log.d("SimpleImageListAdapter", "load failed: $item")
                    val deletedItemIndex = imageList.indexOf(item)
                    MainRepository.getInstance().deleteFromImageList(deletedItemIndex)
                    adapter?.notifyItemRemoved(deletedItemIndex)
                }
            },
            maxImageWidth, getImageDefaultColor()
        ).also {
            adapter = it
        }

    private fun getImageDefaultColor(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            App.getInstance().resources.getColor(R.color.colorImagePreview, App.getInstance().theme)
        } else {
            ContextCompat.getColor(App.getInstance(), R.color.colorImagePreview)
        }
    }

    fun fetchImagesOnQuery(query: String) {
        empty.value = true
        isLastPage = false
        numLoadedPages = 0
        currentQuery = query
        imageBufferFilesDir.listFiles()?.forEach { file ->
            file.delete()
        }
        fetchImagesOnNextPage()
    }

    fun fetchImagesOnNextPage() {
        if (!dataLoading.value!! && !isLastPage) {
            fetchImages(currentQuery, numLoadedPages)
        }
    }

    private fun fetchImages(query: String, page: Int) {
        dataLoading.value = true
        MainRepository.getInstance().getImageData(query, page) { isSuccess, response: ImageData? ->
            dataLoading.value = false
            if (isSuccess) {
                empty.value = false
                if (response?.blocks != null) {
                    val html = response.blocks[0].html
                    val itemList = YandexImageUtil.getSerpListFromHtml(html)
                    newQueryIsLoaded.value = numLoadedPages < 1
                    numLoadedPages++
                    applyData(itemList)
                } else {
                    // TODO check num of pages
                    // TODO show captcha
                    Log.d(tag, "captcha response: $response")
                    shortToast("Требуется ввести капчу")

                    isLastPage = true
                }
            }
        }
    }

    /**
     * Change itemList in repository and and notifies the adapter of changes made
     */
    private fun applyData(itemList: List<SerpItem>) {
        val repo = MainRepository.getInstance()
        if (newQueryIsLoaded.value != false) {
            repo.clearImageList()
            adapter!!.notifyDataSetChanged()
        }
        val lastImageCount = imageCount
        repo.addToImageList(itemList)
        adapter!!.notifyItemRangeInserted(lastImageCount, imageCount - 1)
    }

    companion object {
        private val tag = MainViewModel::class.java.simpleName

        private var INSTANCE: MainViewModel? = null
        fun getInstance() = INSTANCE
            ?: MainViewModel().also {
                INSTANCE = it
            }
    }
}