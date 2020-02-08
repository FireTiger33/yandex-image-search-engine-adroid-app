package com.stacktivity.yandeximagesearchengine.data.model

import com.stacktivity.yandeximagesearchengine.data.model.api.YandexImagesApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainRepository {
    fun getImageData(query: String, onResult: (isSuccess: Boolean, response: ImageData?) -> Unit) {
        Regex("""\s+""").replace(query, "+")
        YandexImagesApi.instance.getJSONSearchResult(search = query).enqueue(object : Callback<ImageData> {
            override fun onResponse(call: Call<ImageData>?, response: Response<ImageData>?) {
                if (response != null && response.isSuccessful) {
                    onResult(true, response.body())
                }
                else
                    onResult(false, null)
            }

            override fun onFailure(call: Call<ImageData>?, t: Throwable?) {
                onResult(false, null)
            }
        })
    }

    companion object {
        private val tag = MainRepository::class.java.simpleName

        private var INSTANCE: MainRepository? = null
        fun getInstance() = INSTANCE
            ?: MainRepository().also {
                INSTANCE = it
            }
    }
}