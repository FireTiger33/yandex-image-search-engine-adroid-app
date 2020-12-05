package com.stacktivity.yandeximagesearchengine.util

import android.graphics.Bitmap
import com.stacktivity.yandeximagesearchengine.data.ColorPixel
import kotlin.random.Random


/**
 * Used to determine the dominant colors in the image.
 *
 * Kotlin Author: Larin Grigoriy <https://github.com/FireTiger33>
 * Created December 1, 2020
 *
 * The calculation uses the k-means method.
 * The idea of this method is to identify clusters of colors in the image
 * and minimize total square deviation of the sample points from cluster centers.
 * Each pixel of the image is considered as a point in the three-dimensional RGB space,
 * where distance to cluster centers of mass is calculated.
 *
 * Material from this source was taken as a basis: http://robocraft.ru/blog/computervision/1063.html
 */
internal object DominantColorsFactory {

    fun getColorsUseKMeans(img: Bitmap, n: Int = 3): List<ColorPixel> {
        val clusters = kMeans(getPoints(img), n)
        return clusters.toList()
    }

    private fun getPoints(img: Bitmap): List<ColorPixel> {
        val points = arrayListOf<ColorPixel>()
        for (i in 0 until img.width) {
            for (j in 0 until img.height) {
                points.add(ColorPixel.from(img.getPixel(i, j)/*.toUInt()*/))
            }
        }

        return points
    }

    private fun calcCenter(pixels: List<ColorPixel>): ColorPixel {
        val countChannels = 3  // number of channels in color space
        val values = IntArray(countChannels) { 0 }
        var pLen = 1
        for (p in pixels) {
            val ct = 1
            pLen += ct
            values[0] += (p.r * ct)
            values[1] += (p.g * ct)
            values[2] += (p.b * ct)
        }
        return ColorPixel((values[0] / pLen), (values[1] / pLen), (values[2] / pLen))
    }

    private fun kMeans(points: List<ColorPixel>, k: Int, minDiff: Double = 1.0): Array<ColorPixel> {
        val r = Random(System.currentTimeMillis())
        val clusters = Array(k) { points[r.nextInt(0, points.size)] }

        while (true) {
            val pLists = Array<ArrayList<ColorPixel>>(k) { arrayListOf() }

            var idx = -1
            for (p in points) {
                var minDist = Double.MAX_VALUE  // inf
                for (i in 0 until k) {
                    val distance = p.euclidean(clusters[i])
                    if (distance < minDist) {
                        minDist = distance
                        idx = i
                    }
                }
                if (idx != -1) {
                    pLists[idx].add(p)
                }
            }

            var diff = 0.0
            for (i in 0 until k) {
                val old = clusters[i]
                val center = calcCenter(pLists[i])
                clusters[i] = center
                diff = kotlin.math.max(diff, old.euclidean(center))
            }

            if (diff < minDiff)
                break
        }

        return clusters
    }
}