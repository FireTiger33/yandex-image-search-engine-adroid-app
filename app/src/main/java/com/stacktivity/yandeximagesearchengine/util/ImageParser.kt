package com.stacktivity.yandeximagesearchengine.util

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL

class ImageParser {
    companion object {
        private val imageLinkRegex: Regex = Regex("((https?:/)?(/([^\\s\"](?!https?))+\\.(jpe?g|png|gif))+)")
        private val protocolNotFoundRegex: Regex = Regex("^/{2}")

        /**
         * Search direct and relative links to images on site.
         *
         * Catches exceptions related to unsupported SSL certificates.
         *
         * @return list of direct image urls
         */
         fun getUrlListToImages(parentUrl: String, onResult: (Collection<String>) -> Unit) {
            CoroutineScope(Dispatchers.IO + Job()).launch(Dispatchers.IO) {
                val linkSet: MutableSet<String> = HashSet()
                val url = URL(parentUrl)
                try {
                    with(BufferedReader(InputStreamReader(url.openStream()))) {
                        var inputLine: String?
                        while (this.readLine().also { inputLine = it } != null) {
                            val lineDataLIst = imageLinkRegex.findAll(inputLine!!)

                            lineDataLIst.forEach { data ->
                                val imageUrl =
                                    if (protocolNotFoundRegex.containsMatchIn(data.value)) {
                                        "${url.protocol}:${data.value}"
                                    } else data.value
                                val dataUrl = try {
                                    URL(imageUrl)
                                } catch (e: MalformedURLException) {
                                    URL(url.protocol, url.host, imageUrl)
                                }

                                linkSet.add(decodeUnicode(dataUrl.toString()))
                            }
                        }
                    }
                } catch (e: IOException) {
                    /**
                     * Possible exceptions:
                     * 1) FileNotFoundException
                     * 2) UnknownHostException
                     * 3) SSLException
                     */
                    e.printStackTrace()
                }

                withContext(Dispatchers.Main) {
                    onResult(linkSet)
                }
            }
        }



        /**
        * Decodes Unicode characters when found.
        *
        * @return decoded string or origin str
        */
        private fun decodeUnicode(str: String): String {
            var res = str
            val unicodeRegex = Regex("\\\\u[a-fA-f0-9]{4}")
            val hexItems: MutableSet<String> = HashSet()

            // find characters in hex representation
            unicodeRegex.findAll(res).forEach { matchResult ->
                hexItems.add(matchResult.value)
            }

            // converting characters from hex representation to normal form
            hexItems.forEach { unicodeHex ->
                val hexVal = unicodeHex.substring(2).toInt(16)
                res = res.replace(unicodeHex, "" + hexVal.toChar())
            }

            // deleting whitespaces
            res = res.replace(Regex("\\s+"), "")

            return res
        }
    }
}