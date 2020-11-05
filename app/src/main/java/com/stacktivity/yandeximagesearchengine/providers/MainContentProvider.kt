package com.stacktivity.yandeximagesearchengine.providers

import com.stacktivity.yandeximagesearchengine.data.ImageItem
import com.stacktivity.yandeximagesearchengine.data.MainRepository

class MainContentProvider {
    companion object : ImageItemsProvider, SubImagesProvider {

        override fun getItemCount(): Int = MainRepository.getInstance().getImageCount()
        override fun getItemOnPosition(position: Int): ImageItem =
                MainRepository.getInstance().getImageOnPosition(position)

        override fun deleteImageItem(itemNum: Int): Int? {
            return MainRepository.getInstance().deleteFromImageList(itemNum)
        }

        override fun loadSubImages(
            mainItemNum: Int,
            onResult: (success: Boolean, errorMsg: String?) -> Unit
        ) {
            MainRepository.getInstance().loadAddImageList(mainItemNum, onResult)
        }

        override fun getSubImagesCount(mainItemNum: Int): Int {
            return MainRepository.getInstance().getAddImageList(mainItemNum).size
        }

        override fun getSubImage(mainItemNum: Int, index: Int): String {
            return MainRepository.getInstance().getAddImageList(mainItemNum)[index]
        }

        override fun deleteSubImage(mainItemNum: Int, imageUrl: String): Int {
            return MainRepository.getInstance()
                    .deleteItemFromAddImageList(mainItemNum, imageUrl)
        }
    }
}