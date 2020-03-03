package com.stacktivity.yandeximagesearchengine.data

import android.util.Log
import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.data.model.YandexResponse
import com.stacktivity.yandeximagesearchengine.data.model.api.YandexImagesApi
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.RuntimeException

class YandexRepository {
    private lateinit var currentCaptchaOnResultCallback: (captchaValue: String) -> Unit
    interface CaptchaEventListener {
        fun onCaptchaEvent(
            captchaImgUrl: String,
            isRepeatEvent: Boolean,
            onResult: (captchaValue: String) -> Unit
        ): Any?
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
                    val body = response?.body()
                    if (body != null && response.isSuccessful) {
                        val captcha = body.captcha
                        if (captcha != null) {
                            currentCaptchaOnResultCallback = { captchaValue ->
                                captcha.sendCaptcha(captchaValue) { isSuccess, responseBody ->
                                    if (!isSuccess) {
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
                    } else
                        onResult(false, null)
                }

                override fun onFailure(call: Call<YandexResponse>?, t: Throwable?) {
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
        eventListener: CaptchaEventListener,
        onResult: (realSource: String?) -> Unit
    ) {
        val yandexCollectionsRegex = Regex("yandex.+?collections")

        if (yandexCollectionsRegex.containsMatchIn(possibleSource)) {
            getImageSourceSiteFromCard(
                possibleSource,
                eventListener
            ) { realSource ->
                onResult(realSource)
            }
        } else {
            onResult(possibleSource)
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
        onResult: (realSource: String?) -> Unit
    ) {
        Log.d(tag, "url: $url")
        YandexImagesApi.instance.getHtml(url).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string()
                if (responseString != null && response.isSuccessful) {
                    getImageSourceSiteFromHtml(responseString)?.let {onResult(it)} ?: run {
                        try {
                            val captcha = Gson().fromJson(responseString, YandexResponse::class.java).captcha

                            if (captcha != null) {
                                currentCaptchaOnResultCallback = { captchaValue ->
                                    captcha.sendCaptcha(captchaValue) { isSuccess, responseBody ->
                                        if (isSuccess) {
                                            onResult(getImageSourceSiteFromHtml(responseBody))
                                        } else {
                                            eventListener.onCaptchaEvent(captcha.img_url, true, currentCaptchaOnResultCallback)
                                        }
                                    }
                                }
                                eventListener.onCaptchaEvent(captcha.img_url, false, currentCaptchaOnResultCallback)
                            } else {
                                onResult(null)
                            }
                        } catch (e: RuntimeException) {
                            onResult(null)
                        }
                    }
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onResult(null)
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

        private var INSTANCE: YandexRepository? = null
        fun getInstance() = INSTANCE
            ?: YandexRepository().also {
                INSTANCE = it
            }
    }
}