package com.stacktivity.yandeximagesearchengine.util

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import com.stacktivity.yandeximagesearchengine.App
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Used for safe operation with the application cache.
 * If there is not enough free space, it deletes older files.
 */
object CacheWorker {
    private val cacheDir: File = App.getInstance().cacheDir
    private var tempFile: File? = null

    fun getFile(fileName: String): File {
        return File(cacheDir.path + File.separator + fileName)
    }


    /**
     * Used to get a temp file that exists before this method is called again
     */
    fun getTempFile(): File {
        tempFile?.delete()
        val name = SimpleDateFormat("ddMMyy_HHmmss", Locale.getDefault()).format(Date())
        return getFile(name)
            .apply { createNewFile() }
            .also { tempFile = it }
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
     * Used to get a file with an explicitly specified extension.
     * If the resulting file already exists, returns it,
     * otherwise a new file will be created.
     */
    fun getFileWithExtension(file: File, filenameExtension: String): File {
        val tempFile = File(file.path + filenameExtension)
        if (!tempFile.exists()) {
            saveBytesToFile(file.readBytes(), tempFile)
        }

        return tempFile
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

    /*@ExperimentalStdlibApi
    private fun getCorrectFilenameExtension(raw: String): String {
        val prefix = if (raw.startsWith('.')) "." else ""
        return prefix + raw.lowercase()
    }*/
}