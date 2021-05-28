package com.stacktivity.yandeximagesearchengine.util

import okhttp3.Response
import java.lang.RuntimeException

/** Exception for an unexpected, non-2xx HTTP response. */
class HttpException(
    val response: Response
) : RuntimeException("HTTP ${response.code()} ${response.message()} ${response.request().url()}") {
    val code: Int get() = response.code()
    val url: String get() = response.request().url().toString()
}