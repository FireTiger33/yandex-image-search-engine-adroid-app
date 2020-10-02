package com.stacktivity.yandeximagesearchengine.ui.main

import android.os.Handler
import android.os.Looper
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
import com.stacktivity.yandeximagesearchengine.ui.adapter.viewHolders.ImageItemViewHolder.EventListener
import com.stacktivity.yandeximagesearchengine.util.Event
import com.stacktivity.yandeximagesearchengine.util.EventForResult
import com.stacktivity.yandeximagesearchengine.util.getString
import com.stacktivity.yandeximagesearchengine.R.string.need_enter_captcha
import java.io.File

class MainViewModel : ViewModel() {
    private val _dataLoading = MutableLiveData<Boolean>().apply { value = false }
    val dataLoading: LiveData<Boolean>
        get() = _dataLoading

    private val _newQueryIsLoaded = MutableLiveData<Boolean>().apply { value = false }
    val newQueryIsLoaded: LiveData<Boolean>
        get() = _newQueryIsLoaded

    private val _captchaEvent = MutableLiveData<EventForResult<String, String>>()
    val captchaEvent: LiveData<EventForResult<String, String>>
        get() = _captchaEvent

    private val _onImageClickEvent = MutableLiveData<Event<String>>()
    val onImageClickEvent: LiveData<Event<String>>
        get() = _onImageClickEvent

    private var numLoadedPages: Int = 0
    private var currentQuery: String = ""
    private var isLastPage = false

    private val imageCount: Int
        get() = MainRepository.getInstance().getImageCount()

    private val imageBufferFilesDir: File
        get() {
            return App.getInstance().cacheDir
        }

    private var adapter: ImageListAdapter? = null

    internal fun getImageItemListAdapter(maxImageWidth: Int): ImageListAdapter {
        return if (adapter != null) {
            adapter!!.onChangeScreenConfiguration(maxImageWidth)
            adapter!!
        } else ImageListAdapter(
            object : ImageListAdapter.ContentProvider {
                override fun getItemCount(): Int = imageCount
                override fun getItemOnPosition(position: Int): ImageItem =
                    MainRepository.getInstance().getImageOnPosition(position)
                override fun getImageRealSourceSite(
                    item: ImageItem,
                    onAsyncResult: (realSource: String?, errorMsg: String?) -> Unit
                ) {
                    val possibleSource: String = item.sourceSite
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
            object : EventListener {
                override fun onImageLoadFailed(item: ImageItem) {
                    MainRepository.getInstance().deleteFromImageList(item)?.let {
                        adapter?.notifyItemRemoved(it)
                    }
                }

                override fun onAdditionalImageClick(imageUrl: String) {
                    _onImageClickEvent.value = Event(imageUrl)
                }
            },
            maxImageWidth
        ).also {
            adapter = it
        }
    }

    fun fetchImagesOnQuery(query: String) {
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
        // start loading
        _dataLoading.value = true
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
            // loading complete
            _dataLoading.value = false
            if (isSuccess) {
                if (response?.blocks != null) {
                    val html = response.blocks[0].html
                    val itemList = YandexImageUtil.getImageItemListFromHtml(
                        lastIndex = if (page > 0) MainRepository.getInstance().getImageCount() else 0,
                        html = html
                    )
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
     * Change itemList in repository and notifies the adapter of changes made
     */
    private fun applyData(itemList: List<ImageItem>) {
        if (newQueryIsLoaded.value != false) {
            clearCache()
            adapter!!.onDataClear()
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