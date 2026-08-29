package com.armsone.denimdex.core.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.sqrt

data class ScoredPhoto(
    val index: Int,
    val data: ByteArray,
    val sharpnessScore: Double,
    val thumbnailFingerprint: LongArray
)

object PhotoDeduplicator {

    /**
     * Selects up to [targetCount] best photos from [photos], removing duplicates based on
     * thumbnail difference and prioritizing higher sharpness.
     */
    fun selectBestPhotos(
        photos: List<ByteArray>,
        targetCount: Int = QuickValueImagePolicy.SEND_MAXIMUM_COUNT
    ): List<ByteArray> {
        if (photos.size <= targetCount) return photos

        val scored = photos.mapIndexed { index, bytes ->
            val (sharpness, fingerprint) = analyzePhoto(bytes)
            ScoredPhoto(
                index = index,
                data = bytes,
                sharpnessScore = sharpness,
                thumbnailFingerprint = fingerprint
            )
        }

        // Deduplicate similar photos
        val retained = mutableListOf<ScoredPhoto>()
        for (candidate in scored) {
            val duplicate = retained.find { isSimilar(it.thumbnailFingerprint, candidate.thumbnailFingerprint) }
            if (duplicate == null) {
                retained.add(candidate)
            } else {
                // If similar, keep the sharper one
                if (candidate.sharpnessScore > duplicate.sharpnessScore) {
                    val idx = retained.indexOf(duplicate)
                    retained[idx] = candidate
                }
            }
        }

        // If still more than targetCount, keep the sharpest targetCount photos in original index order
        val best = if (retained.size > targetCount) {
            retained.sortedByDescending { it.sharpnessScore }
                .take(targetCount)
                .sortedBy { it.index }
        } else {
            retained.sortedBy { it.index }
        }

        return best.map { it.data }
    }

    private fun analyzePhoto(bytes: ByteArray): Pair<Double, LongArray> {
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4 // Downsample for fast analysis
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: return 0.0 to LongArray(16)

            val sharpness = calculateSharpness(bitmap)
            val fingerprint = computeFingerprint(bitmap)
            bitmap.recycle()
            sharpness to fingerprint
        } catch (_: Exception) {
            0.0 to LongArray(16)
        }
    }

    /**
     * Approximates Laplacian variance sharpness on grayscale pixels.
     */
    private fun calculateSharpness(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return 0.0

        val sampleStep = maxOf(1, minOf(width, height) / 32)
        var totalLaplacian = 0.0
        var count = 0

        for (y in 1 until height - 1 step sampleStep) {
            for (x in 1 until width - 1 step sampleStep) {
                val center = getLuminance(bitmap.getPixel(x, y))
                val top = getLuminance(bitmap.getPixel(x, y - 1))
                val bottom = getLuminance(bitmap.getPixel(x, y + 1))
                val left = getLuminance(bitmap.getPixel(x - 1, y))
                val right = getLuminance(bitmap.getPixel(x + 1, y))

                val laplacian = abs(4 * center - top - bottom - left - right)
                totalLaplacian += laplacian * laplacian
                count++
            }
        }

        return if (count > 0) totalLaplacian / count else 0.0
    }

    private fun computeFingerprint(bitmap: Bitmap): LongArray {
        // Create an 8x8 average luminance thumbnail fingerprint
        val scaled = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val result = LongArray(8)
        for (y in 0 until 8) {
            var rowBits = 0L
            for (x in 0 until 8) {
                val lum = getLuminance(scaled.getPixel(x, y))
                if (lum > 128) {
                    rowBits = rowBits or (1L shl x)
                }
            }
            result[y] = rowBits
        }
        if (scaled !== bitmap) scaled.recycle()
        return result
    }

    private fun isSimilar(fp1: LongArray, fp2: LongArray): Boolean {
        var diffBits = 0
        for (i in 0 until minOf(fp1.size, fp2.size)) {
            diffBits += java.lang.Long.bitCount(fp1[i] xor fp2[i])
        }
        return diffBits <= 6 // < 10% bit distance indicates very similar framing
    }

    private fun getLuminance(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}
