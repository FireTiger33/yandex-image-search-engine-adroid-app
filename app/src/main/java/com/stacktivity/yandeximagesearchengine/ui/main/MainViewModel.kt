package com.stacktivity.yandeximagesearchengine.ui.main

import com.stacktivity.yandeximagesearchengine.base.BaseViewModel
import com.stacktivity.yandeximagesearchengine.data.model.ImageData
import com.stacktivity.yandeximagesearchengine.data.model.MainRepository

class MainViewModel : BaseViewModel() {

    fun fetchImages() {
        dataLoading.value = true
        MainRepository.getInstance().getRepoList { isSuccess, response: ImageData? ->
            dataLoading.value = false
            if (isSuccess) {
                empty.value = false
            } else {
                empty.value = true
            }
        }
    }

    companion object {
        private val tag = MainViewModel::class.java.simpleName
    }
}