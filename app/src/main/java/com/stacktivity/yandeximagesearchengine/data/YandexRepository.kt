package com.stacktivity.yandeximagesearchengine.data

import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.data.model.YandexResponse
import com.stacktivity.yandeximagesearchengine.data.model.api.YandexImagesApi
import com.stacktivity.yandeximagesearchengine.util.NetworkStateReceiver
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class YandexRepository {
    private lateinit var currentCaptchaOnResultCallback: (captchaValue: String) -> Unit

    interface CaptchaEventListener {
        fun onCaptchaEvent(
            captchaImgUrl: String,
            isRepeatEvent: Boolean,
            onResult: (captchaValue: String) -> Unit
        )
    }

    fun getImageData(
        query: String, page: Int,
        onResult: (isSuccess: Boolean, response: YandexResponse?) -> Unit
    ) {
        getImageData(
            query, page,
            captchaEventListener?: throw captchaEventListenerException,
            onResult
        )
    }

    fun getImageData(
        query: String, page: Int,
        eventListener: CaptchaEventListener,
        onResult: (isSuccess: Boolean, response: YandexResponse?) -> Unit
    ) {
        Regex("""\s+""").replace(query, "+")
        YandexImagesApi.instance.getJSONSearchResult(search = query, page = page)
            .enqueue(object : Callback<YandexResponse> {
                override fun onResponse(
                    call: Call<YandexResponse>?,
                    response: Response<YandexResponse>?
                ) {
                    response?.body()?.let { body ->
                        val captcha = body.captcha

                        if (captcha != null) {
                            currentCaptchaOnResultCallback = { captchaValue ->
                                captcha.sendCaptcha(captchaValue) { responseBody ->
                                    if (responseBody == null) {
                                        eventListener.onCaptchaEvent(captcha.img_url, true, currentCaptchaOnResultCallback)
                                    } else {
                                        val newResponse = Gson().fromJson(responseBody, YandexResponse::class.java)
                                        onResult(true, newResponse)
                                    }
                                }
                            }
                            eventListener.onCaptchaEvent(captcha.img_url, false, currentCaptchaOnResultCallback)
                        } else {
                            onResult(true, response.body())
                        }
                    }?: run {
                        onResult(false, null)
                    }
                }

                override fun onFailure(call: Call<YandexResponse>?, t: Throwable?) {
                    NetworkStateReceiver.getInstance().post {
                        getImageData(query, page, eventListener, onResult)
                    }
                }
            })
    }

    companion object {
        val tag = YandexRepository::class.java.simpleName
        private var captchaEventListener: CaptchaEventListener? = null
        private val captchaEventListenerException =
            IllegalStateException("Captcha event listener is not registered")
        private var INSTANCE: YandexRepository? = null
        fun registerCaptchaEventListener(captchaEventListener: CaptchaEventListener) {
            this.captchaEventListener = captchaEventListener
        }
        fun getInstance() = INSTANCE
            ?: YandexRepository().also {
                INSTANCE = it
            }
    }
}