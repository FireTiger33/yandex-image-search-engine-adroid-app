package com.stacktivity.yandeximagesearchengine.data

import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.data.model.Captcha
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
            captchaEventListener ?: throw captchaEventListenerException,
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
            .enqueue(getRepeatedOnFailureYandexResponseCallback(
                eventListener,
                onSuccess = { body -> onResult(true, body) },
                onFailure = { onResult(false, null) },
                repeatedCall = { getImageData(query, page, eventListener, onResult) }
            ))
    }

    fun getImageDataByImage(
        imageUrl: String, page: Int,
        onResult: (isSuccess: Boolean, response: YandexResponse?) -> Unit
    ) {
        getImageDataByImage(
            imageUrl, page,
            captchaEventListener ?: throw captchaEventListenerException,
            onResult
        )
    }

    fun getImageDataByImage(
        url: String, page: Int,
        eventListener: CaptchaEventListener,
        onResult: (isSuccess: Boolean, response: YandexResponse?) -> Unit
    ) {
        Regex("""\s+""").replace(url, "+")
        YandexImagesApi.instance.getJSONSearchResultOnImage(url = url, page = page)
            .enqueue(getRepeatedOnFailureYandexResponseCallback(
                eventListener,
                onSuccess = { body -> onResult(true, body) },
                onFailure = { onResult(false, null) },
                repeatedCall = { getImageData(url, page, eventListener, onResult) }
            ))
    }

    private fun sendCaptchaEvent(
        captcha: Captcha,
        listener: CaptchaEventListener,
        doOnSuccess: (YandexResponse) -> Unit
    ) {
        currentCaptchaOnResultCallback = { captchaValue ->
            captcha.sendCaptcha(captchaValue) { responseBody ->
                if (responseBody == null) {
                    listener.onCaptchaEvent(captcha.img_url, true, currentCaptchaOnResultCallback)
                } else {
                    val newResponse = Gson().fromJson(responseBody, YandexResponse::class.java)
                    doOnSuccess(newResponse)
                }
            }
        }
        listener.onCaptchaEvent(captcha.img_url, false, currentCaptchaOnResultCallback)
    }

    private fun getRepeatedOnFailureYandexResponseCallback(
        captchaEventListener: CaptchaEventListener,
        onSuccess: (YandexResponse) -> Unit,
        onFailure: () -> Unit,
        repeatedCall: () -> Unit
    ) = object : Callback<YandexResponse> {
        override fun onResponse(call: Call<YandexResponse>, response: Response<YandexResponse>) {
            response.body()?.let { body ->
                val captcha = body.captcha
                if (captcha != null) {
                    sendCaptchaEvent(captcha, captchaEventListener) { newResponse ->
                        onSuccess(newResponse)
                    }
                } else {
                    onSuccess(body)
                }
            } ?: run { onFailure() }
        }

        override fun onFailure(call: Call<YandexResponse>, t: Throwable) {
            NetworkStateReceiver.getInstance().post { repeatedCall() }
        }
    }

    companion object {
        val tag: String = YandexRepository::class.java.simpleName
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