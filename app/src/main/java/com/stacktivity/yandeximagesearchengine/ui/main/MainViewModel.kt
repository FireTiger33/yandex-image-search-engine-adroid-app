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
import com.stacktivity.yandeximagesearchengine.data.model.Blocks
import com.stacktivity.yandeximagesearchengine.data.model.YandexResponse
import com.stacktivity.yandeximagesearchengine.providers.MainContentProvider
import com.stacktivity.yandeximagesearchengine.ui.adapter.ImageListAdapter
import com.stacktivity.yandeximagesearchengine.util.EventForResult
import kotlinx.coroutines.*
import java.io.File

class MainViewModel : ViewModel(), YandexRepository.CaptchaEventListener {
    private val _dataLoading = MutableLiveData<Boolean>().apply { value = false }
    val dataLoading: LiveData<Boolean>
        get() = _dataLoading

    private val _newQueryIsLoaded = MutableLiveData<Boolean>().apply { value = false }
    val newQueryIsLoaded: LiveData<Boolean>
        get() = _newQueryIsLoaded

    private val _captchaEvent = MutableLiveData<EventForResult<String, String>>()
    val captchaEvent: LiveData<EventForResult<String, String>>
        get() = _captchaEvent

    private var numLoadedPages: Int = 0
    private var currentQuery: String = ""
    private var isLastPage = false

    private val imageBufferFilesDir: File = App.getInstance().cacheDir

    private var adapter: ImageListAdapter? = null

    init {
        YandexRepository.registerCaptchaEventListener(this)
    }

    internal fun getImageItemListAdapter(maxImageWidth: Int): ImageListAdapter {
        return if (adapter != null) {
            adapter!!.onChangeScreenConfiguration(maxImageWidth)
            adapter!!
        } else ImageListAdapter(
            MainContentProvider,
            MainContentProvider,
            imageBufferFilesDir.path,
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

    override fun onCaptchaEvent(
        captchaImgUrl: String,
        isRepeatEvent: Boolean,
        onResult: (captchaValue: String) -> Unit
    ) {
        Handler(Looper.getMainLooper()).post {
            _captchaEvent.value = EventForResult(captchaImgUrl, isRepeatEvent) { result ->
                if (result != null) {
                    onResult(result)
                } /*else {
                    onAsyncResult(null, getString(R.string.need_enter_captcha))
                }*/
            }
        }
    }

    private fun fetchImages(query: String, page: Int) {
        // start loading
        _dataLoading.value = true
        YandexRepository.getInstance()
            .getImageData(query, page) { isSuccess, response: YandexResponse? ->
                // loading complete
                if (isSuccess) {
                    response?.blocks?.let {
                        val isNewQuery = page == 0
                        onFetchComplete(it[0], isNewQuery) {
                            _dataLoading.value = false
                            _newQueryIsLoaded.value = isNewQuery
                        }
                    } ?: kotlin.run {
                        isLastPage = true
                    }
                }
            }
    }

    private fun onFetchComplete(imageBlock: Blocks, isNewQuery: Boolean, onSuccess: () -> Unit) {
        val html = imageBlock.html
        numLoadedPages++
        GlobalScope.launch(Dispatchers.Default) {
            val itemList = YandexImageUtil.getImageItemListFromHtml(
                html = html,
                startIndexingItemsFromScratch = isNewQuery
            )
            if (numLoadedPages == imageBlock.params.lastPage) {
                isLastPage = true
            }
            applyData(itemList, isNewQuery)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    /**
     * Change itemList in repository and notifies the adapter of changes made
     */
    private fun applyData(itemList: List<ImageItem>, isNewQuery: Boolean) = runBlocking {
        if (isNewQuery) {
            val clearCacheJob = async(Dispatchers.IO) { clearCache() }
            MainRepository.getInstance().clearAllData()
            MainRepository.getInstance().addToImageList(itemList)
            clearCacheJob.await()
            withContext(Dispatchers.Main) { adapter!!.onReloadData() }
        } else {
            val itemCount = MainContentProvider.getItemCount()
            MainRepository.getInstance().addToImageList(itemList)
            withContext(Dispatchers.Main) {
                adapter!!.notifyItemRangeInserted(itemCount, MainContentProvider.getItemCount() - 1)
            }
        }
    }

    private fun clearCache() {
        imageBufferFilesDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }

    companion object {
        val tag: String = MainViewModel::class.java.simpleName

        private var INSTANCE: MainViewModel? = null
        fun getInstance() = INSTANCE
            ?: MainViewModel().also {
                INSTANCE = it
            }
    }
}