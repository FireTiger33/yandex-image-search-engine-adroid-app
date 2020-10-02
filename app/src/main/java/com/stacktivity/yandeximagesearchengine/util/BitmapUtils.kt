package com.stacktivity.yandeximagesearchengine.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.stacktivity.yandeximagesearchengine.util.FileWorker.Companion.createFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class BitmapUtils {

    companion object {
        private val tag = BitmapUtils::class.java.simpleName

        /**
         * Checks whether file has a known image format.
         *
         * If the file has an unknown mimetype (non-image), it returns false.
         */
        fun fileIsAnImage(filePath: String): Boolean {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, options)

            return options.outMimeType != null
        }

        /**
         * Checks whether file is a gif image
         */
        fun fileIsAnGifImage(filePath: String): Boolean {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, options)

            return options.outMimeType == "image/gif"
        }

        /**
         * Simplifies Bitmap to desired resolution
         */
        fun getSimplifiedBitmap(imagePath: String, reqWidth: Int = -1, reqHeight: Int = -1): Bitmap? {
            val validReqWidth: Int
            val validReqHeight: Int
            val cropFactor: Float
            val options = BitmapFactory.Options()

            if (reqWidth < 1 && reqHeight < 1) {
                return null
            }

            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imagePath, options)
            if (options.outMimeType == null) {
                return null
            }

            if (reqHeight < 0) {
                validReqWidth = reqWidth
                cropFactor = reqWidth / options.outWidth.toFloat()
                validReqHeight = (options.outHeight * cropFactor).toInt()
            } else {
                validReqHeight = reqHeight
                cropFactor = reqHeight / options.outHeight.toFloat()
                validReqWidth = (options.outWidth * cropFactor).toInt()
            }

            options.inSampleSize = calculateInSampleSize(options, validReqWidth, validReqHeight)
            options.inJustDecodeBounds = false

            return BitmapFactory.decodeFile(imagePath, options)
        }

        /**
         * Non-blocking function for saving Bitmap to file
         * without loss of quality
         */
        fun saveBitmapToFile(bitmap: Bitmap, destFile: File) =
                GlobalScope.launch(Dispatchers.IO) {
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos)

                    if (!destFile.exists()) {
                        if (!createFile(destFile)) {
                            return@launch
                        }
                    }

                    try {
                        with(FileOutputStream(destFile)) {
                            this.write(bos.toByteArray())
                        }
                    } catch (e: IOException) {
                        // TODO delete cache
                        Log.e(tag, "saveBitmapToFile: I/O error")
                    }
                }

        fun getBitmapFromFile(imageFile: File): Bitmap? {
            Log.i(tag, "get bitmap from: ${imageFile.path}")
            var res: Bitmap? = null
            try {
                res = BitmapFactory.decodeFile(imageFile.path, null)
            } catch (e: OutOfMemoryError) {
                Log.e(tag, "OutOfMemory on get bitmap from file")
            }

            return res
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