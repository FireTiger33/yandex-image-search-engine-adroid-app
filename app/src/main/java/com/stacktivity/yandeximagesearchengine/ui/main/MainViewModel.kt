package com.stacktivity.yandeximagesearchengine.ui.main

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stacktivity.yandeximagesearchengine.App
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.util.YandexImageUtil
import com.stacktivity.yandeximagesearchengine.base.BaseViewModel
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.data.MainRepository
import com.stacktivity.yandeximagesearchengine.data.YandexRepository
import com.stacktivity.yandeximagesearchengine.data.model.*
import com.stacktivity.yandeximagesearchengine.ui.adapter.ImageListAdapter
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.ImageItemViewHolder
import com.stacktivity.yandeximagesearchengine.util.Event
import java.io.File

class MainViewModel : BaseViewModel() {
    private val _newQueryIsLoaded = MutableLiveData<Boolean>().apply { value = false }
    val newQueryIsLoaded: LiveData<Boolean>
        get() = _newQueryIsLoaded
    private val _captchaEvent = MutableLiveData<Event<String, String>>()
    val captchaEvent: LiveData<Event<String, String>>
        get() = _captchaEvent
    private var numLoadedPages: Int = 0
    private var currentQuery: String = ""
    private var isLastPage = false

    private val imageList: List<ImageItem>
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
                override fun getItemOnPosition(position: Int): ImageItem = imageList[position]
                override fun getImageRealSourceSite(
                    possibleSource: String,
                    onAsyncResult: (realSource: String?) -> Unit
                ) {
                    YandexRepository.getInstance().getImageRealSourceSite(
                        possibleSource,
                        object : YandexRepository.CaptchaEventListener {
                            override fun onCaptchaEvent(
                                captchaImgUrl: String,
                                isRepeatEvent: Boolean,
                                onResult: (captchaValue: String) -> Unit
                            ) = Handler(Looper.getMainLooper()).post {
                                _captchaEvent.value =
                                    Event(captchaImgUrl, isRepeatEvent) { result ->
                                        if (result != null) {
                                            onResult(result)
                                        } else {
                                            onAsyncResult(null)
                                        }
                                    }
                            }
                        },
                        onAsyncResult
                    )
                }

                override fun setAddImageList(position: Int, list: List<String>) {
                    AddImagesRepository.getInstance().createAddImageList(position, list)
                }

                override fun getAddImagesCountOnPosition(position: Int): Int {
                    return AddImagesRepository.getInstance().getAddImageList(position).size
                }

                override fun getAddImageListItemOnPosition(position: Int, itemIndex: Int): String {
                    return AddImagesRepository.getInstance().getAddImageList(position)[itemIndex]
                }

                override fun deleteItemOtherImageOnPosition(position: Int, imageUrl: String): Int {
                    return AddImagesRepository.getInstance()
                        .deleteItemFromAddImageList(position, imageUrl)
                }
            },
            imageBufferFilesDir,
            object : ImageItemViewHolder.EventListener {
                override fun onImageLoadFailed(item: ImageItem) {
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
        clearCache()
        fetchImagesOnNextPage()
    }

    fun fetchImagesOnNextPage() {
        if (!dataLoading.value!! && !isLastPage) {
            fetchImages(currentQuery, numLoadedPages)
        }
    }

    private fun fetchImages(query: String, page: Int) {
        dataLoading.value = true
        YandexRepository.getInstance().getImageData(
            query, page,
            object : YandexRepository.CaptchaEventListener {
                override fun onCaptchaEvent(
                    captchaImgUrl: String,
                    isRepeatEvent: Boolean,
                    onResult: (captchaValue: String) -> Unit
                ) = Handler(Looper.getMainLooper()).post {
                    _captchaEvent.value = Event(captchaImgUrl, isRepeatEvent) { result ->
                        if (result != null) {
                            onResult(result)
                        }
                    }
                }
            }) { isSuccess, response: YandexResponse? ->
            dataLoading.value = false
            if (isSuccess) {
                empty.value = false
                if (response?.blocks != null) {
                    val html = response.blocks[0].html
                    val itemList = YandexImageUtil.getImageItemListFromHtml(html)
                    _newQueryIsLoaded.value = numLoadedPages < 1
                    numLoadedPages++
                    if (numLoadedPages == response.blocks[0].params.lastPage) {
                        isLastPage = true
                    }
                    applyData(itemList)
                } else {
                    isLastPage = true
                }
            }
        }
    }

    /**
     * Change itemList in repository and and notifies the adapter of changes made
     */
    private fun applyData(itemList: List<ImageItem>) {
        val repo = MainRepository.getInstance()
        if (_newQueryIsLoaded.value != false) {
            repo.clearImageList()
            adapter!!.notifyDataSetChanged()
        }
        val lastImageCount = imageCount
        repo.addToImageList(itemList)
        adapter!!.notifyItemRangeInserted(lastImageCount, imageCount - 1)
    }

    private fun clearCache() {
        imageBufferFilesDir.listFiles()?.forEach { file ->
            file.delete()
        }
        AddImagesRepository.getInstance().clearData()
    }

    companion object {
        val tag = MainViewModel::class.java.simpleName

        private var INSTANCE: MainViewModel? = null
        fun getInstance() = INSTANCE
            ?: MainViewModel().also {
                INSTANCE = it
            }
    }
}