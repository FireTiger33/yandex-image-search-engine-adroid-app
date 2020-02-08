package com.stacktivity.yandeximagesearchengine.ui.main

import androidx.lifecycle.MutableLiveData
import com.stacktivity.yandeximagesearchengine.util.YandexImageUtil
import com.stacktivity.yandeximagesearchengine.base.BaseViewModel
import com.stacktivity.yandeximagesearchengine.data.model.ImageData
import com.stacktivity.yandeximagesearchengine.data.model.MainRepository
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem

class MainViewModel : BaseViewModel() {
    val imageListLive = MutableLiveData<List<SerpItem>>()

    fun fetchImages() {
        dataLoading.value = true
        MainRepository.getInstance().getRepoList { isSuccess, response: ImageData? ->
            dataLoading.value = false
            if (isSuccess) {
                empty.value = false
                if (response != null) {
                    val html = response.blocks[0].html

                    imageListLive.value = YandexImageUtil.getSerpListFromHtml(html)
                }
            } else {
                empty.value = true
            }
        }
    }

    companion object {
        private val tag = MainViewModel::class.java.simpleName
    }
}