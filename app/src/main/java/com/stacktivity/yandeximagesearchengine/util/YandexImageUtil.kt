package com.stacktivity.yandeximagesearchengine.util

import com.google.gson.Gson
import com.stacktivity.yandeximagesearchengine.data.ImageData
import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.data.model.Preview
import com.stacktivity.yandeximagesearchengine.data.model.SerpItem
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_HEIGHT
import com.stacktivity.yandeximagesearchengine.util.Constants.Companion.MIN_IMAGE_WIDTH

object YandexImageUtil {
    private var lastItemIndex = 0

    /**
     * Retrieves raw data and prepares it by filtering
     * duplicates and images that are too small
     *
     * @param html the html code from the Yandex images page
     *
     * @return list of ImageItem with prepared data for future use
     */
    fun getImageItemListFromHtml(
        html: String,
        startIndexingItemsFromScratch: Boolean = false
    ): List<ImageItem> {
        val imageList: ArrayList<ImageItem> = arrayListOf()
        var imageItem: ImageItem
        if (startIndexingItemsFromScratch) lastItemIndex = 0

        getSerpListFromHtml(html).forEachIndexed { itemNum, item ->
            val allImages: ArrayList<ImageData> = ArrayList()
            (item.preview + item.dups + item.thumb.toPreview())
                .distinctBy { it.origin?.url ?: it.url }
                .filter(this::imageResolutionFilter)
                .mapTo(allImages) { ImageData(it) }

            if (allImages.isNotEmpty()) {
                imageItem = ImageItem(
                    itemNum + lastItemIndex,
                    item.snippet.title,
                    item.snippet.url,
                    allImages.apply {
                        sortByDescending { it.width }
                    },
                    item.thumb
                )

                imageList.add(imageItem)
            }
        }

        lastItemIndex += imageList.size

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
            serpItem.thumb = serpItem.thumb.copy(url = "https:" + serpItem.thumb.url)
            itemList.add(serpItem)
        }

        return itemList
    }

    private fun imageResolutionFilter(preview: Preview): Boolean {
        return preview.origin?.w ?: preview.w > MIN_IMAGE_WIDTH
            && preview.origin?.h ?: preview.h > MIN_IMAGE_HEIGHT
    }
}

private fun ImageData(preview: Preview) = ImageData(
    width = preview.origin?.w ?: preview.w,
    height = preview.origin?.h ?: preview.h,
    fileSizeInBytes = preview.fileSizeInBytes,
    url = (preview.origin?.url ?: preview.url).replace("amp;", "")
)