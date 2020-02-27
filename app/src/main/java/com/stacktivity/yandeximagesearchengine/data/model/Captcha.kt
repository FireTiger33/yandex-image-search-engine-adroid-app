package com.stacktivity.yandeximagesearchengine.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.stacktivity.yandeximagesearchengine.data.model.api.YandexImagesApi
import kotlinx.android.synthetic.main.captcha_dialog.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

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

    fun sendCaptcha(
        value: String,
        onResult: (isSuccess: Boolean, responseBody: String?) -> Unit
    ) {
        if (!isInit) {
            initAddFields()
            isInit = true
        }

        val validValue = Regex("""\s+""").replace(value, "+")

        GlobalScope.launch(Dispatchers.IO) {
            val response = YandexImagesApi.instance.sendYandexCaptchaForHtml(
                host = host,
                returnUrl = returnPath,
                key = key,
                value = validValue,
                format = ""
            ).execute()

            val body: String? = response.body()?.string()

            if (Regex("captcha").containsMatchIn(body ?: "")) {
                YandexImagesApi.instance.sendYandexCaptchaForHtml(
                    host = host,
                    returnUrl = returnPath,
                    key = key,
                    value = validValue,
                    format = "json"
                ).execute().body()?.string().let {
                    val yaCaptcha = Gson().fromJson(it, YandexResponse::class.java).captcha
                    if (yaCaptcha != null) {
                        key = yaCaptcha.key
                        img_url = yaCaptcha.img_url
                        status = yaCaptcha.status
                    }
                }

                onResult(false, null)
            } else {
                onResult(response.isSuccessful, body)
            }
        }
    }
}