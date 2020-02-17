package com.stacktivity.yandeximagesearchengine.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel() {
    val empty = MutableLiveData<Boolean>().apply { value = true }
    val dataLoading = MutableLiveData<Boolean>().apply { value = false }
}