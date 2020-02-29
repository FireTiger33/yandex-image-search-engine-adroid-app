package com.stacktivity.yandeximagesearchengine.data

class MainRepository {
    private val imageList = ArrayList<ImageItem>()
    private val addImageMapList: HashMap<Int, ArrayList<String>> = hashMapOf()

    fun getImageList(): ArrayList<ImageItem> = imageList

    fun getImageCount(): Int = imageList.size

    fun addToImageList(itemList: List<ImageItem>) = imageList.addAll(itemList)

    fun deleteFromImageList(index: Int) {
        imageList.removeAt(index)
    }

//    fun clearImageList() = imageList.clear()

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