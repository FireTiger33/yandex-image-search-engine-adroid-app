package com.stacktivity.yandeximagesearchengine.data.model.api

import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.BuildConfig.DEBUG
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.BASE_YANDEX_IMAGES_URL
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.REQUEST_TIMEOUT_DURATION
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object YandexImagesApi {

    val instance: ApiService = Retrofit.Builder().run {
        baseUrl(BASE_YANDEX_IMAGES_URL)
        addConverterFactory(GsonConverterFactory.create(Gson()))
        client(createRequestInterceptorClient())
        build()
    }.create(ApiService::class.java)


    private fun createRequestInterceptorClient(): OkHttpClient {
        val interceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            val request = requestBuilder.build()
            chain.proceed(request)
        }

        return getOkHttpClient(interceptor)
    }

    private fun getOkHttpClient(interceptor: Interceptor): OkHttpClient {
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(REQUEST_TIMEOUT_DURATION.toLong(), TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT_DURATION.toLong(), TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT_DURATION.toLong(), TimeUnit.SECONDS)

        if (DEBUG) {
            clientBuilder
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }

        return clientBuilder.build()
    }
}