package com.stacktivity.yandeximagesearchengine.ui.main

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stacktivity.yandeximagesearchengine.App
import com.stacktivity.yandeximagesearchengine.util.YandexImageUtil
import com.stacktivity.yandeximagesearchengine.util.NetworkStateReceiver
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.data.MainRepository
import com.stacktivity.yandeximagesearchengine.data.YandexRepository
import com.stacktivity.yandeximagesearchengine.data.model.YandexResponse
import com.stacktivity.yandeximagesearchengine.ui.adapter.ImageListAdapter
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.ImageItemViewHolder
import com.stacktivity.yandeximagesearchengine.util.Event
import com.stacktivity.yandeximagesearchengine.util.EventForResult
import com.stacktivity.yandeximagesearchengine.util.getString
import com.stacktivity.yandeximagesearchengine.R.string.need_enter_captcha
import java.io.File

class MainViewModel : ViewModel() {
    val empty = MutableLiveData<Boolean>().apply { value = true }
    val dataLoading = MutableLiveData<Boolean>().apply { value = false }
    val newQueryIsLoaded = MutableLiveData<Boolean>().apply { value = false }

    private val _captchaEvent = MutableLiveData<EventForResult<String, String>>()
    val captchaEvent: LiveData<EventForResult<String, String>>
        get() = _captchaEvent

    private val _onImageClickEvent = MutableLiveData<Event<String>>()
    val onImageClickEvent: LiveData<Event<String>>
        get() = _onImageClickEvent

    private var numLoadedPages: Int = 0
    private var currentQuery: String = ""
    private var isLastPage = false

    private val imageList: List<ImageItem>
        get() = MainRepository.getInstance().getImageList()
    private val imageCount: Int
        get() = MainRepository.getInstance().getImageCount()

    private val imageBufferFilesDir: File
        get() {
            return App.getInstance().cacheDir
        }

    private var adapter: ImageListAdapter? = null

    fun getImageItemListAdapter(maxImageWidth: Int): ImageListAdapter = adapter
        ?: ImageListAdapter(
            object : ImageListAdapter.ContentProvider {
                override fun getItemCount(): Int = imageCount
                override fun getItemOnPosition(position: Int): ImageItem = imageList[position]
                override fun getImageRealSourceSite(
                    possibleSource: String,
                    onAsyncResult: (realSource: String?, errorMsg: String?) -> Unit
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
                                    EventForResult(captchaImgUrl, isRepeatEvent) { result ->
                                        if (result != null) {
                                            onResult(result)
                                        } else {
                                            onAsyncResult(null, getString(need_enter_captcha))
                                        }
                                    }
                            }
                        },
                        onAsyncResult
                    )
                }

                override fun setAddImageList(position: Int, list: List<String>) {
                    MainRepository.getInstance().createAddImageList(position, list)
                }

                override fun getAddImagesCountOnPosition(position: Int): Int {
                    return MainRepository.getInstance().getAddImageList(position).size
                }

                override fun getAddImageListItemOnPosition(position: Int, itemIndex: Int): String {
                    return MainRepository.getInstance().getAddImageList(position)[itemIndex]
                }

                override fun deleteItemOtherImageOnPosition(position: Int, imageUrl: String): Int {
                    return MainRepository.getInstance()
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

                override fun onAdditionalImageClick(imageUrl: String) {
                    _onImageClickEvent.value = Event(imageUrl)
                }
            },
            maxImageWidth
        ).also {
            adapter = it
        }

    fun fetchImagesOnQuery(query: String) {
        empty.value = true
        isLastPage = false
        numLoadedPages = 0
        currentQuery = query
        NetworkStateReceiver.getInstance().removeAllListeners()
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
                    _captchaEvent.value = EventForResult(captchaImgUrl, isRepeatEvent) { result ->
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
                    newQueryIsLoaded.value = numLoadedPages < 1
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
        if (newQueryIsLoaded.value != false) {
            clearCache()
            adapter!!.notifyDataSetChanged()
        }
        val lastImageCount = imageCount
        MainRepository.getInstance().addToImageList(itemList)
        adapter!!.notifyItemRangeInserted(lastImageCount, imageCount - 1)
    }

    private fun clearCache() {
        imageBufferFilesDir.listFiles()?.forEach { file ->
            file.delete()
        }
        MainRepository.getInstance().clearAllData()
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