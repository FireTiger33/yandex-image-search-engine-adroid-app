package com.stacktivity.yandeximagesearchengine.util

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileWorker {
    companion object {

        /**
         * Create new file
         *
         * @return true if the file creation was successful
         * @return false otherwise
         */
        fun createFile(file: File): Boolean {
            var createSuccess = false
            try {
                createSuccess = file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return createSuccess
        }

        /**
         * Write list of string to file with line separator.
         *
         * If file not exists, it will be created.
         *
         * @return true if success
         */
        fun saveStringListToFile(list: List<String>, file: File): Boolean {
            var res = false

            if (!file.exists()) {
                if (!createFile(file)) {
                    return res
                }
            }

            FileOutputStream(file).use {
                list.forEach { line ->
                    it.write((line + System.lineSeparator()).toByteArray())
                }
                res = true
            }

            return res
        }

        /**
         * Read lines from file to list of string
         */
        fun loadStringListFromFile(file: File): List<String> {
            val list = arrayListOf<String>()

            if (file.exists()) {
                with(file.reader()) {
                    list.addAll(this.readLines())
                }
            }

            return list
        }
    }
}