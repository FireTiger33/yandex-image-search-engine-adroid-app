package com.stacktivity.yandeximagesearchengine.data

class MainRepository {
    private val imageList = ArrayList<ImageItem>()

    fun getImageList(): ArrayList<ImageItem> = imageList

    fun getImageCount(): Int = imageList.size

    fun addToImageList(itemList: List<ImageItem>) = imageList.addAll(itemList)

    fun deleteFromImageList(index: Int) {
        imageList.removeAt(index)
    }

    fun clearImageList() = imageList.clear()

    companion object {
        private val tag = MainRepository::class.java.simpleName

        private var INSTANCE: MainRepository? = null
        fun getInstance() = INSTANCE
            ?: MainRepository().also {
                INSTANCE = it
            }
    }
}