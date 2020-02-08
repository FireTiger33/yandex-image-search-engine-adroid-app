package com.stacktivity.yandeximagesearchengine.util

import android.util.Log
import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.data.model.Preview
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem

class YandexImageUtil {
    companion object {
        private val tag = YandexImageUtil::class.java.simpleName

        /**
         * Convert html class serp-list to list of SerpItem data class
         */
        fun getSerpListFromHtml(html: String): List<SerpItem> {
            val itemList = arrayListOf<SerpItem>()
            val regItem = Regex("""data-bem='..serp-item.:(.+?),"detail_url".+?'""")
            val dataList = regItem.findAll(html)

            /*data?.groupValues?.forEach { itemField ->
                Log.d(tag, "Field: $itemField")
            }*/

            dataList.forEachIndexed { i, data ->
                val resultJSON = data.groupValues[1] + "}"
                val serpItem: SerpItem = Gson().fromJson(resultJSON, SerpItem::class.java)
                itemList.add(serpItem)

                // DebugInfo
                Log.d(tag, "Thumb_${i}_Url: ${serpItem.thumb.url}")
                val firstPreview: Preview = serpItem.preview.first()
                val originUrl: String = firstPreview.origin?.url ?: firstPreview.url
                Log.d(tag, "OriginImage_${i}_Url: $originUrl")
            }

            return itemList
        }
    }
}