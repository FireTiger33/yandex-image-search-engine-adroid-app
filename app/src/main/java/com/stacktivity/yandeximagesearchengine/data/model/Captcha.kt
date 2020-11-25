package com.stacktivity.yandeximagesearchengine.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.stacktivity.yandeximagesearchengine.data.model.api.YandexImagesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class Captcha(
    @SerializedName("img-url")
    var img_url: String,
    var key: String,
    var status: String,
    @SerializedName("captcha-page")
    val captcha_page: String,
    var isInit: Boolean = true
) {

    private lateinit var host: String
    private lateinit var returnPath: String

    private fun initAddFields() {
        val retPathMatch = Regex("retpath=(.+?)&").find(captcha_page)
        returnPath = if (retPathMatch != null) {
            retPathMatch.groupValues[1]
        } else {
            throw IllegalStateException("Captcha does not contain return path in captcha page link")
        }
        host = URL(img_url).host
    }


    private suspend fun sendCaptcha(
        captchaValue: String, getJson: Boolean
    ): String? = suspendCoroutine {
        val validCaptchaValue = Regex("""\s+""").replace(captchaValue, "+")
        val format = if (getJson) "json" else ""

        YandexImagesApi.instance.sendYandexCaptchaForHtml(
            host = host,
            returnUrl = returnPath,
            key = key,
            value = validCaptchaValue,
            format = format
        ).enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                it.resumeWithException(t)
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                it.resume(response.body()?.string())
            }

        })
    }


    /**
     * Send captcha and return result in main thread
     */
    fun sendCaptcha(
        value: String,
        onResult: (responseBody: String?) -> Unit
    ) {
        if (!isInit) {
            initAddFields()
            isInit = true
        }

        var result: String? = null

        GlobalScope.launch(Dispatchers.IO) {
            sendCaptcha(value, getJson = false)?.let { stringBody ->
                if (stringBody.contains("captcha")) {  // TODO mapper from ResponseBody to Captcha
                    sendCaptcha(value, getJson = true)?.let { captchaJson ->
                        val yaCaptcha =
                            Gson().fromJson(captchaJson, YandexResponse::class.java).captcha
                        if (yaCaptcha != null) {
                            key = yaCaptcha.key
                            img_url = yaCaptcha.img_url
                            status = yaCaptcha.status
                        }
                    }
                } else {
                    result = stringBody
                }
            }

            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
}