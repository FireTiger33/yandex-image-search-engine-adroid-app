package com.stacktivity.yandeximagesearchengine.data.model

class AddImagesRepository {
    private val addImageMapList: HashMap<Int, ArrayList<String>> = hashMapOf()

    fun createAddImageList(index: Int, list: List<String>) {
        addImageMapList[index] = ArrayList(list)
    }

    fun getAddImageList(index: Int): List<String> {
        return addImageMapList[index]?: listOf()
    }


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


    fun clearData() {
        addImageMapList.keys.forEach { key ->
            addImageMapList[key]?.clear()
        }

        addImageMapList.clear()
    }

    companion object {
        private val tag = AddImagesRepository::class.java.simpleName

        private var INSTANCE: AddImagesRepository? = null
        fun getInstance() = INSTANCE
            ?: AddImagesRepository().also {
                INSTANCE = it
            }
    }
}