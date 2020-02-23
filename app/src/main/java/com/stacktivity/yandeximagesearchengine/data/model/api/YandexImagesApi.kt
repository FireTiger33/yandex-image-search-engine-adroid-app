package com.stacktivity.yandeximagesearchengine.data.model.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.stacktivity.yandeximagesearchengine.BuildConfig.DEBUG
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.BASE_YANDEX_URL
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.REQUEST_READ_TIMEOUT_DURATION
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.REQUEST_TIMEOUT_DURATION
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object YandexImagesApi {
    private val tag = YandexImagesApi::class.java.simpleName

    val instance: ApiService = Retrofit.Builder().run {
        baseUrl(BASE_YANDEX_URL)
        addConverterFactory(GsonConverterFactory.create(getGson()))
        client(getOkHttpClient(getEventListener()))
        build()
    }.create(ApiService::class.java)


    private fun getGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    private fun getEventListener(): EventListener {
        return object: EventListener() {
            override fun callFailed(call: Call, ioe: IOException) {
                Log.d(tag, "connectionFailed: ${ioe.message}\ncheck internet connection")
            }
        }
    }

    private fun getOkHttpClient(eventListener: EventListener): OkHttpClient {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(REQUEST_TIMEOUT_DURATION.toLong(), TimeUnit.SECONDS)
            .readTimeout(REQUEST_READ_TIMEOUT_DURATION.toLong(), TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT_DURATION.toLong(), TimeUnit.SECONDS)
            .eventListener(eventListener)

        if (DEBUG) {
            clientBuilder
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }

        return clientBuilder.build()
    }
}