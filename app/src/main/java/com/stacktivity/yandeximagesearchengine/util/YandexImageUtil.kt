package com.stacktivity.yandeximagesearchengine.util

import android.util.Log
import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

class YandexImageUtil {
    companion object {
        private val tag = YandexImageUtil::class.java.simpleName

        /**
         * Convert html class serp-list to list of SerpItem data class
         *
         * @param html the html code from the Yandex images page
         */
        fun getSerpListFromHtml(html: String): List<SerpItem> {
            val itemList = arrayListOf<SerpItem>()
            val regItem = Regex("""data-bem='..serp-item.:(.+?),"detail_url".+?'""")
            val dataList = regItem.findAll(html)

            dataList.forEach { data ->
                val resultJSON = data.groupValues[1] + "}"
                val serpItem: SerpItem = Gson().fromJson(resultJSON, SerpItem::class.java)
                itemList.add(serpItem)
            }

            return itemList
        }


        /**
         * If image source refers to Yandex collections,
         * an attempt is made to install real source.
         *
         * @return link to image source or null if source could not be found
         */
        suspend fun getImageSourceSite(item: SerpItem): String? {
            var source = item.snippet.url
            var originSource: String?
            val yandexCollectionsRegex = Regex("yandex.+?collections")

            while (yandexCollectionsRegex.containsMatchIn(source)) {
                originSource = getImageSourceSiteFromCard(source)
                if (originSource != null) {
                    source = originSource
                } else {
                    return null
                }
            }

            return source
        }


        /**
         * Search link to source site from the Yandex collections page.
         *
         * Catches exceptions related to unsupported SSL certificates.
         *
         * @return link to image source or null if source could not be found
         */
        private suspend fun getImageSourceSiteFromCard(url: String): String? =
            withContext(Dispatchers.IO) {
                Log.d(tag, "url: $url")
                val res: String?
                val sourceSiteReg = Regex("page_url.:.(.+?)..,")
                var matchRes: MatchResult? = null

                try {
                    with(BufferedReader(InputStreamReader(URL(url).openStream()))) {
                        var inputLine: String?
                        while (this.readLine().also { inputLine = it } != null) {
                            matchRes = sourceSiteReg.find(inputLine!!)
                            if (matchRes != null) {
                                break
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                res = matchRes?.groupValues?.get(1)

                return@withContext res
            }
    }
}