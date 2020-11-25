package com.stacktivity.yandeximagesearchengine.util

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import com.stacktivity.yandeximagesearchengine.App

/**
 * Used for safe operation with the application cache.
 * If there is not enough free space, it deletes older files.
 */
object CacheWorker {
    private val cacheDir: File = App.getInstance().cacheDir

    fun getFile(fileName: String): File {
        return File(cacheDir.path + File.separator + fileName)
    }

    /**
     * Save [kotlin.ByteArray] to file
     *
     * @return false in case of an [IOException]
     */
    fun saveBytesToFile(buffer: ByteArray, file: File): Boolean {
        var res = false
        val currentFreeSpace = cacheDir.freeSpace

        if (currentFreeSpace > buffer.size) {
            res = saveBytes(buffer, file)
        } else if (clearCache(buffer.size - currentFreeSpace)) {
            res = saveBytes(buffer, file)
        }

        return res
    }

    private fun saveBytes(buffer: ByteArray, file: File): Boolean {
        var res = false

        try {
            file.writeBytes(buffer)
            res = true
        } catch (e: IOException) {
            // res = false
            e.printStackTrace()
        }

        return res
    }

    /**
     * Save [ByteBuffer] to file
     *
     * @return false in case of an [IOException]
     */
    fun saveBytesToFile(buffer: ByteBuffer, file: File) {
        buffer.rewind()
        val array = ByteArray(buffer.remaining())
        buffer.get(array)
        saveBytesToFile(array, file)
    }

    /**
     * Deletes oldest files in cache until [byteCount] are cleared
     */
    fun clearCache(byteCount: Long): Boolean {
        var clearBytes = 0L
        cacheDir.listFiles()?.let {
            it.sortBy { file -> file.lastModified() }

            for (file in it) {
                if (clearBytes > byteCount) break
                clearBytes += file.length()
                file.delete()
            }
        }

        return false
    }

    fun clearAllCache() {
        cacheDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }
}