package com.stacktivity.yandeximagesearchengine.data

import android.util.Log
import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.data.model.YandexResponse
import com.stacktivity.yandeximagesearchengine.data.model.api.YandexImagesApi
import com.stacktivity.yandeximagesearchengine.R.string
import com.stacktivity.yandeximagesearchengine.util.getString
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalStateException
import java.lang.RuntimeException

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
                    // TODO create networkListener and enqueue when Internet access is available
                    onResult(false, null)
                }
            })
    }

    /**
     * If image source refers to Yandex collections,
     * an attempt is made to get real source.
     *
     * @return link to image source or null if source could not be found
     */
    fun getImageRealSourceSite(
        possibleSource: String,
        onResult: (realSource: String?, errorMsg: String?) -> Unit
    ) {
        val yandexCollectionsRegex = Regex("yandex.+?/collections")

        if (yandexCollectionsRegex.containsMatchIn(possibleSource)) {
            getImageSourceSiteFromCard(
                possibleSource,
                captchaEventListener?: throw captchaEventListenerException
            ) { realSource, errorMsg ->
                onResult(realSource, errorMsg)
            }
        } else {
            onResult(possibleSource, null)
        }
    }


    /**
     * Search link to source site from the Yandex collections page.
     *
     * If you receive an unsatisfactory result from the server,
     * it is checked for the presence of captcha and called [CaptchaEventListener.onCaptchaEvent],
     * which is passed to the [Unit], waiting for the entered characters from the captcha
     *
     * @return link to image source or null if source could not be found
     */
    private fun getImageSourceSiteFromCard(
        url: String,
        eventListener: CaptchaEventListener,
        onResult: (realSource: String?, errorMsg: String?) -> Unit
    ) {
        Log.d(tag, "card-url: $url")
        YandexImagesApi.instance.getHtml(url).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string()
                print("responseString = $responseString, success = ${response.isSuccessful}")
                if (responseString != null && response.isSuccessful) {
                    getImageSourceSiteFromHtml(responseString)?.let { print("SourceSite = null"); onResult(it, null)} ?: run {
                        try {
                            val captcha = Gson().fromJson(responseString, YandexResponse::class.java).captcha

                            if (captcha != null) {
                                currentCaptchaOnResultCallback = { captchaValue ->
                                    captcha.sendCaptcha(captchaValue) { responseBody ->
                                        if (responseBody != null) {
                                            onResult(getImageSourceSiteFromHtml(responseBody), null)
                                        } else {
                                            eventListener.onCaptchaEvent(
                                                captchaImgUrl = captcha.img_url,
                                                isRepeatEvent = true,
                                                onResult = currentCaptchaOnResultCallback
                                            )
                                        }
                                    }
                                }
                                eventListener.onCaptchaEvent(captcha.img_url, false, currentCaptchaOnResultCallback)
                            } else {
                                onResult(null, getString(string.server_invalid_response))
                            }
                        } catch (e: RuntimeException) {
                            onResult(null, getString(string.could_not_find_image_real_source))
                        }
                    }
                } else {
                    onResult(null, getString(string.response_is_not_success).format(response.code()))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onResult(null, getString(string.server_is_not_responding))
            }
        })
    }

    private fun getImageSourceSiteFromHtml(html: String?): String? {
        val sourceSiteReg = Regex("page_url.:.(.+?)..,")
        val matchRes: MatchResult? = html?.let { sourceSiteReg.find(it) }

        return matchRes?.groupValues?.get(1)
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