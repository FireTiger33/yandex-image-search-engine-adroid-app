package com.stacktivity.yandeximagesearchengine.util

import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_WIDTH

class YandexImageUtil {

    companion object {
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
                        allImages.add(
                            ImageData(
                                preview.w, preview.h,
                                preview.fileSizeInBytes,
                                preview.url
                            )
                        )
                    }
                }

                imageItem =
                    ImageItem(
                        item.snippet.title,
                        item.snippet.url,
                        allImages.sortedByDescending { it.width }
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
    }
}