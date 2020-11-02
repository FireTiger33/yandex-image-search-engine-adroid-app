package com.stacktivity.yandeximagesearchengine.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.stacktivity.yandeximagesearchengine.util.FileWorker.Companion.createFile
import kotlinx.coroutines.*
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
        suspend fun fileIsAnImage(filePath: String): Boolean = withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, options)

            return@withContext options.outMimeType != null
        }

        /**
         * Checks whether file is a gif image
         */
        suspend fun fileIsAnGifImage(filePath: String): Boolean = withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, options)

            return@withContext options.outMimeType == "image/gif"
        }

        fun getImageSize(file: File): Pair<Int, Int> {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, options)
            if (options.outMimeType != null) {  // file is image
                return Pair(options.outWidth, options.outHeight)
            } else {
                throw IllegalArgumentException("File is not an image")
            }
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
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch(Dispatchers.IO) {
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos)

                    if (!destFile.exists()) {
                        if (!createFile(destFile)) {
                            return@launch
                        }
                    }

                    try {
                        FileOutputStream(destFile).use {
                            it.write(bos.toByteArray())
                        }
                    } catch (e: IOException) {
                        // TODO delete cache
                        Log.e(tag, "saveBitmapToFile: I/O error")
                    }
                }

        fun getBitmapFromFile(imageFile: File, onResult: suspend (Bitmap?) -> Unit)
                = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            var res: Bitmap? = null
            try {
                res = BitmapFactory.decodeFile(imageFile.path, null)
            } catch (e: OutOfMemoryError) {
                Log.e(tag, "OutOfMemory on get bitmap from file")
            } finally {
                onResult(res)
            }
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

        fun blur(image: Bitmap): Bitmap {
            val startMs = System.currentTimeMillis()
            val scaleFactor = 1f
            val radius = 2f/*20f*/
            /*if (downScale.isChecked()) {
                scaleFactor = 8f
                radius = 2f
            }*/
            var overlay = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(overlay)
            val paint = Paint()

            canvas.scale(1 / scaleFactor, 1 / scaleFactor)
            paint.flags = Paint.FILTER_BITMAP_FLAG
            canvas.drawBitmap(image, 0f, 0f, paint)
            overlay = FastBlur.doBlur(overlay, radius.toInt(), true)
            Log.d(tag, "Blur ${System.currentTimeMillis() - startMs}ms")

            return overlay
        }
    }
}