package com.stacktivity.yandeximagesearchengine.data

import android.util.ArrayMap
import com.stacktivity.yandeximagesearchengine.util.ImageParser

class MainRepository {
    private val imageList: ArrayMap<Int, ImageItem> = ArrayMap()
    private val addImageMapList: HashMap<Int, MutableList<String>> = hashMapOf()
    private var putIndex = 0

    fun getImageOnPosition(position: Int): ImageItem {
        return imageList.valueAt(position)
    }

    fun getImageCount(): Int = imageList.size

    fun addToImageList(itemList: List<ImageItem>) {
        imageList.putAll(itemList.mapIndexed { i, imageItem -> Pair(i + putIndex, imageItem) })
        putIndex += itemList.size
    }

    /**
     * Removes an [ImageItem] from storage
     *
     * @return sequence number of deleted item
     */
    fun deleteFromImageList(itemNum: Int): Int? {
        val res = imageList.indexOfKey(itemNum)
        imageList.remove(itemNum)

        return res
    }

    fun loadAddImageList(index: Int, onAsyncResult: (success: Boolean, errorMsg: String?) -> Unit) {
        val possibleSource: String = imageList[index]?.sourceSite ?: run {
            onAsyncResult(false, "Item with index $index does not exist")
            return
        }

        if (addImageMapList[index] != null) {
            onAsyncResult(true, null)
        } else {

            YandexRepository.getInstance()
                .getImageRealSourceSite(possibleSource) { realSource, errorMsg ->

                    if (realSource != null) {
                        imageList[index]?.sourceSite = realSource
                        ImageParser.getUrlListToImages(realSource) { urls ->
                            addImageMapList[index] = ArrayList(urls)
                            onAsyncResult(true, null)
                        }
                    } else {
                        onAsyncResult(false, errorMsg)
                    }
                }
        }
    }

    fun getAddImageList(index: Int): List<String> = addImageMapList[index] ?: listOf()

    /**
     * Deletes an item from one of the lists by value.
     *
     * @return number of the deleted item in list or -1 if item not found
     */
    fun deleteItemFromAddImageList(listNum: Int, value: String): Int {
        val deletedItemIndex = addImageMapList[listNum]?.indexOf(value) ?: -1
        if (deletedItemIndex > -1) {
            addImageMapList[listNum]?.removeAt(deletedItemIndex)
        }

        return deletedItemIndex
    }


    fun clearAllData() {
        imageList.clear()

        addImageMapList.keys.forEach { key ->
            addImageMapList[key]?.clear()
        }

        addImageMapList.clear()
        putIndex = 0
    }

    companion object {
        private val tag = MainRepository::class.java.simpleName

        private var INSTANCE: MainRepository? = null
        fun getInstance() = INSTANCE
            ?: MainRepository().also {
                INSTANCE = it
            }
    }
}