package com.stacktivity.yandeximagesearchengine.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import javax.net.ssl.SSLException

class ImageParser {
    companion object {
        private val imageLinkRegex: Regex = Regex("((https?:/)?/[^\\s\"]*?\\.(jpe?g|png))")

        /**
         * Search direct and relative links to images on site.
         *
         * Catches exceptions related to unsupported SSL certificates.
         *
         * @return list of direct image urls
         */
        suspend fun getUrlListToImages(parentUrl: String): ArrayList<String> =
            withContext(Dispatchers.IO) {
                val linkList = arrayListOf<String>()
                val url = URL(parentUrl)
                try {
                    with(BufferedReader(InputStreamReader(url.openStream()))) {
                        var inputLine: String?
                        while (this.readLine().also { inputLine = it } != null) {
                            val lineDataLIst = imageLinkRegex.findAll(inputLine!!)

                            lineDataLIst.forEach { data ->
                                val dataUrl = try {
                                    URL(data.value)
                                } catch (e: MalformedURLException) {
                                    URL(url.protocol, url.host, data.value)
                                }

                                linkList.add(dataUrl.toString())
                            }

                        }
                    }
                } catch (e: SSLException) {
                    e.printStackTrace()
                }

                return@withContext linkList
            }
    }
}