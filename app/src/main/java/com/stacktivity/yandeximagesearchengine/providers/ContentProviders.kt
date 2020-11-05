package com.stacktivity.yandeximagesearchengine.providers

import com.stacktivity.yandeximagesearchengine.data.ImageItem

interface ImageItemsProvider {
    fun getItemCount(): Int
    fun getItemOnPosition(position: Int): ImageItem

    /**
     * @return sequence number of deleted [ImageItem]
     */
    fun deleteImageItem(itemNum: Int): Int?
}

interface SubImagesProvider {
    fun loadSubImages(mainItemNum: Int, onResult: (success: Boolean, errorMsg: String?) -> Unit)
    fun getSubImagesCount(mainItemNum: Int): Int
    fun getSubImage(mainItemNum: Int, index: Int): String
    fun deleteSubImage(mainItemNum: Int, imageUrl: String): Int
}