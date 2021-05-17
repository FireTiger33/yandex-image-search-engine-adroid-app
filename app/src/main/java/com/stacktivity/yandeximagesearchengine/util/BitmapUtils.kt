package com.stacktivity.yandeximagesearchengine.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.stacktivity.yandeximagesearchengine.data.ColorPixel
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteBuffer

class BitmapUtils {

    companion object {
        private val mScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mComputingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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

        /**
         * Checks whether the buffer contains a "GIF" header
         */
        fun bufferIsAGif(buffer: ByteBuffer): Boolean {
            if (buffer.capacity() < 4) {
                return false
            }
            val head = ByteArray(3)
            buffer.get(head, 0, 3)
            buffer.rewind()

            return head.toString(Charsets.UTF_8) == "GIF"
        }

        suspend fun getImageSize(file: File): Pair<Int, Int> = withContext(Dispatchers.IO) {  // TODO remove
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, options)
            if (options.outMimeType != null) {  // file is image
                Pair(options.outWidth, options.outHeight)
            } else {
                throw IllegalArgumentException("File is not an image")
            }
        }

        /**
         * Used to determine the format of an image by its header
         * Recognizes formats such as GIF, Jpeg, PNG and BMP
         *
         * @param header - image header (min size is 10) or full image in [CharArray]
         * @return .{image file format} or empty string if unknown format
         */
        fun getImageFormat(header: CharArray) = when {
            header.concatToString(0, 3) == "GIF" -> ".gif"
            header.concatToString(1, 4) == "PNG" -> ".png"
            header.concatToString(6, 10) == "JFIF" -> ".jpg"
            header.concatToString(0, 2) == "BM" -> ".bmp"
            else -> "".also { Log.e(tag, "unknown image format ${header.contentToString()}") }
        }


        /**
         * Used to determine the image format by [file] header
         * Recognizes formats such as GIF, Jpeg, PNG, and BMP
         *
         * @param file - image file
         * @return image filename extension or empty string
         */
        fun getImageFormat(file: File): String {
            val headerBuff = CharArray(10)
            file.reader().read(headerBuff, 0, 10)
            return getImageFormat(headerBuff)
        }

        /**
         * Simplifies Bitmap to desired resolution
         */
        suspend fun getSimplifiedBitmap(
            imagePath: String,
            reqWidth: Int = -1, reqHeight: Int = -1,
            onResult: suspend (Bitmap?) -> Unit
        ) = CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            val validReqWidth: Int
            val validReqHeight: Int
            val cropFactor: Float
            val options = BitmapFactory.Options()

            if (reqWidth < 1 && reqHeight < 1) {
                onResult(null)
                return@launch
            }

            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imagePath, options)
            if (options.outMimeType == null) {
                onResult(null)
                return@launch
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

            try {
                onResult(BitmapFactory.decodeFile(imagePath, options))
            } catch (e: OutOfMemoryError) {
                onResult(null)
            }
        }

        fun getBitmapFromFile(imageFile: File, onResult: suspend (Bitmap?) -> Unit) =
            mScope.launch {
                var res: Bitmap? = null
                Log.d(tag, "getBitmapFromFile thread: ${Thread.currentThread().name}")  // TODO remove
                try {
                    res = BitmapFactory.decodeFile(imageFile.path, null)
                } catch (e: OutOfMemoryError) {
                    Log.e(tag, "OutOfMemory on get bitmap from file")
                } finally {
                    withContext(Dispatchers.Main.immediate) {
                        onResult(res)
                    }
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

        /**
         * Apply fast Gaussian blur to image
         *
         * @return blurred copy of the original image
         */
        fun blur(image: Bitmap): Bitmap {
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

            return overlay
        }

        /**
         * Use for getting [Bitmap] from [ByteBuffer]
         * with ability to get a smaller image if needed.
         * If you try to get a larger image, original image will be returned.
         *
         * If only one of required resolution parameters is set, image will change proportionally
         *
         * @param reqWidth  required width
         * @param reqHeight required height
         *
         * @return The decoded bitmap, or null if the image could not be decoded
         */
        fun getBitmap(buffer: ByteBuffer, reqWidth: Int? = null, reqHeight: Int? = null): Bitmap? {
            buffer.rewind()
            val byteArray = ByteArray(buffer.remaining())
            buffer.get(byteArray)
            val preResult: Bitmap? = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

            return if (preResult != null && (reqWidth != null || reqHeight != null)) {
                // Calc out image size
                val mReqWidth: Int = reqWidth
                    ?: (reqHeight!!.toFloat() / preResult.height * preResult.width).toInt()
                val mReqHeight: Int = reqHeight
                    ?: (reqWidth!!.toFloat() / preResult.width * preResult.height).toInt()

                if (mReqWidth >= preResult.width) {
                    preResult
                } else {
                    Bitmap.createScaledBitmap(preResult, mReqWidth, mReqHeight, false)
                }
            } else {
                preResult
            }
        }


        fun getDominantColors(image: Bitmap, n: Int, onResult: (List<ColorPixel>) -> Unit) {
            mComputingScope.launch {
                val res = DominantColorsFactory.getColorsUseKMeans(image, n)

                withContext(Dispatchers.Main) {
                    onResult(res)
                }
            }
        }
    }
}