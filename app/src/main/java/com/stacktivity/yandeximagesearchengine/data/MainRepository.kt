package com.stacktivity.yandeximagesearchengine.data

import com.stacktivity.yandeximagesearchengine.data.model.YandexResponse
import com.stacktivity.yandeximagesearchengine.data.model.api.YandexImagesApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainRepository {
    private val imageList = ArrayList<ImageItem>()

    fun getImageData(query: String, page: Int,
                     onResult: (isSuccess: Boolean, response: YandexResponse?) -> Unit) {
        Regex("""\s+""").replace(query, "+")
        YandexImagesApi.instance.getJSONSearchResult(search = query, page = page).enqueue(object : Callback<YandexResponse> {
            override fun onResponse(call: Call<YandexResponse>?, response: Response<YandexResponse>?) {
                if (response != null && response.isSuccessful) {
                    onResult(true, response.body())
                }
                else
                    onResult(false, null)
            }

            override fun onFailure(call: Call<YandexResponse>?, t: Throwable?) {
                onResult(false, null)
            }
        })
    }

    fun getImageList(): ArrayList<ImageItem> = imageList

    fun getImageCount(): Int = imageList.size

    fun addToImageList(itemList: List<ImageItem>) = imageList.addAll(itemList)

    fun deleteFromImageList(index: Int) {
        imageList.removeAt(index)
    }

    fun clearImageList() = imageList.clear()

    companion object {
        private val tag = MainRepository::class.java.simpleName

        private var INSTANCE: MainRepository? = null
        fun getInstance() = INSTANCE
            ?: MainRepository().also {
                INSTANCE = it
            }
    }
}