package com.stacktivity.yandeximagesearchengine.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.stacktivity.yandeximagesearchengine.util.FileWorker.Companion.createFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class BitmapUtils {

    companion object {
        private val tag = BitmapUtils::class.java.simpleName

        /**
         * Simplifies Bitmap to desired resolution
         */
        fun getSimplifiedBitmap(imagePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
            val options = BitmapFactory.Options()

            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imagePath, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            return BitmapFactory.decodeFile(imagePath, options)
        }

        suspend fun saveBitmapToFile(bitmap: Bitmap, destFile: File): Boolean =
            withContext(Dispatchers.IO) {
                var res = false
                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                val inputByteArray = bos.toByteArray()

                if (!destFile.exists()) {
                    if (createFile(destFile)) {
                        try {
                            with(FileOutputStream(destFile)) {
                                this.write(inputByteArray)
                                Log.d(tag, "image save complete: ${destFile.path}")
                            }
                            res = true
                        } catch (e: IOException) {
                            Log.e(tag, "saveBitmapToFile: I/O error")
                        }
                    }
                }

                return@withContext res
            }

        fun getBitmapFromFile(imageFile: File): Bitmap? {  // TODO custom resolution
            Log.d(tag, "get bitmap from: ${imageFile.path}")
            return BitmapFactory.decodeFile(imageFile.path, null)
        }

        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
}