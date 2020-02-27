package com.stacktivity.yandeximagesearchengine.data

import android.util.Log
import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.data.model.YandexResponse
import com.stacktivity.yandeximagesearchengine.data.model.api.YandexImagesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    ) = GlobalScope.launch {
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
     * Catches exceptions related to unsupported SSL certificates.
     *
     * @return link to image source or null if source could not be found
     */
    private suspend fun getImageSourceSiteFromCard(
        url: String,
        eventListener: CaptchaEventListener,
        onResult: (realSource: String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        Log.d(tag, "url: $url")
        val responseBody = YandexImagesApi.instance.getHtml(url).execute().body()
        var result =  if (responseBody != null) {
            getImageSourceSiteFromHtml(responseBody.string())
        } else {
            onResult(null)
            return@withContext
        }

        if (result == null) {
            val yaResponse = YandexImagesApi.instance.getYandexResponse(url).execute()
            val captcha = yaResponse.body()?.captcha

            if (captcha != null) {
                currentCaptchaOnResultCallback = { captchaValue ->
                    captcha.sendCaptcha(captchaValue) { isSuccess, responseBody ->
                        if (!isSuccess) {
                            eventListener.onCaptchaEvent(captcha.img_url, true, currentCaptchaOnResultCallback)
                        } else {
                            result = getImageSourceSiteFromHtml(responseBody)
                            if (result != null) {
                                onResult(result)
                            } else {
                                onResult(null)
                            }
                        }
                    }
                }
                eventListener.onCaptchaEvent(captcha.img_url, false, currentCaptchaOnResultCallback)
            } else {
                onResult(null)
            }
        } else {
            onResult(result)
        }
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