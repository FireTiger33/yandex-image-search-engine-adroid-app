package com.stacktivity.yandeximagesearchengine.util

import android.util.Log
import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem
import com.stacktivity.yandeximagesearchengine.data.model.api.YandexImagesApi
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_WIDTH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YandexImageUtil {

    companion object {
        private val tag = YandexImageUtil::class.java.simpleName

        /**
         * Retrieves raw data and prepares it by filtering
         * duplicates and images that are too small
         *
         * @param html the html code from the Yandex images page
         *
         * @return list of ImageItem with prepared data for future use
         */
        fun getImageItemListFromHtml(html: String): List<ImageItem> {
            val imageList: ArrayList<ImageItem> = arrayListOf()
            var imageItem: ImageItem
            getSerpListFromHtml(html).forEach { item ->
                val allImages: ArrayList<ImageData> = ArrayList()
                val allPreview = (item.preview + item.dups)
                    .distinctBy { it.origin?.url?: it.url }
                    .filter { it.origin?.w?: it.w > MIN_IMAGE_WIDTH }
                allPreview.forEach { preview ->
                    if (preview.origin != null) {
                        allImages.add(
                            ImageData(
                                preview.origin.w, preview.origin.h,
                                preview.fileSizeInBytes,
                                preview.origin.url
                            )
                        )
                    } else {
                        Log.d(tag, "origin = null: $preview \n all: $item")
                        allImages.add(
                            ImageData(
                                preview.w, preview.h,
                                preview.fileSizeInBytes,
                                preview.url
                            )
                        )
                    }
                }
                allImages.sortedByDescending { it.width }

                imageItem =
                    ImageItem(
                        item.snippet.title,
                        item.snippet.url,
                        allImages
                    )

                imageList.add(imageItem)
            }

            return imageList
        }

        /**
         * Convert html class serp-list to list of SerpItem data class
         *
         * @param html the html code from the Yandex images page
         */
        private fun getSerpListFromHtml(html: String): List<SerpItem> {
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
        suspend fun getImageRealSourceSite(possibleSource: String): String? {
            var source = possibleSource
            var originSource: String?
            val yandexCollectionsRegex = Regex("yandex.+?collections")
            var isNotSourceOfImage = true

            if (yandexCollectionsRegex.containsMatchIn(source)) {
                while (isNotSourceOfImage) {
                    originSource = getImageSourceSiteFromCard(source)
                    if (originSource != null) {
                        source = originSource
                        isNotSourceOfImage = false
                    } else {
                        return null
                    }
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
                    val response = YandexImagesApi.instance.getHtml(url).execute()
                    matchRes = sourceSiteReg.find(response.body()!!)
                } catch (e: IllegalStateException) {
                    // TODO process result and show captcha
                }

                res = matchRes?.groupValues?.get(1)

                return@withContext res
            }
    }
}