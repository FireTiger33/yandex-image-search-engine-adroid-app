package com.stacktivity.yandeximagesearchengine.data

import android.util.ArrayMap

class MainRepository {
    private val imageList: ArrayMap<Int, ImageItem> = ArrayMap()
    private val addImageMapList: HashMap<Int, ArrayList<String>> = hashMapOf()
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
    fun deleteFromImageList(item: ImageItem): Int? {
        val res = imageList.indexOfKey(item.itemNum)
        imageList.remove(item.itemNum)

        return res
    }


    /**
     * Create a separate list for additional images
     */
    fun createAddImageList(index: Int, list: List<String>) {
        addImageMapList[index] = ArrayList(list)
    }

    fun getAddImageList(index: Int): List<String> = addImageMapList[index]?: listOf()

    /**
     * Deletes an item from one of the lists by value.
     *
     * @return number of the deleted item in list or -1 if item not found
     */
    fun deleteItemFromAddImageList(listNum: Int, value: String): Int {
        val deletedItemIndex = addImageMapList[listNum]?.indexOf(value)?: -1
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