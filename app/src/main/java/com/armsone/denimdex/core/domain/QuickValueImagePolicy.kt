package com.armsone.denimdex.core.domain

data class ImageDimensionAndQuality(
    val maxLongEdgePixels: Int,
    val initialJpegQuality: Int,
    val minimumJpegQuality: Int = 52
)

object QuickValueImagePolicy {
    const val CAPTURE_MAXIMUM_COUNT: Int = 30
    const val SEND_MAXIMUM_COUNT: Int = 20
    const val TOTAL_BATCH_BUDGET_BYTES: Long = 16_000_000L
    const val MAXIMUM_BYTES_PER_IMAGE: Long = 2_000_000L
    const val MINIMUM_LONG_EDGE_PIXELS: Int = 640

    fun policyForCount(imageCount: Int): ImageDimensionAndQuality {
        val count = imageCount.coerceIn(1, SEND_MAXIMUM_COUNT)
        return when {
            count in 1..8 -> ImageDimensionAndQuality(
                maxLongEdgePixels = 2048,
                initialJpegQuality = 84,
                minimumJpegQuality = 52
            )
            count in 9..12 -> ImageDimensionAndQuality(
                maxLongEdgePixels = 1792,
                initialJpegQuality = 82,
                minimumJpegQuality = 52
            )
            count in 13..16 -> ImageDimensionAndQuality(
                maxLongEdgePixels = 1600,
                initialJpegQuality = 80,
                minimumJpegQuality = 52
            )
            else -> ImageDimensionAndQuality(
                maxLongEdgePixels = 1536,
                initialJpegQuality = 78,
                minimumJpegQuality = 52
            )
        }
    }
}
