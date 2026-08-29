package com.armsone.denimdex.core.domain

import com.armsone.denimdex.core.model.JapanSaleRange
import com.armsone.denimdex.core.model.KoreaSaleRange
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class NetProceedsRange(
    val low: Long,
    val high: Long
)

data class MarginRange(
    val low: Long,
    val high: Long
)

enum class CrossMarketRecommendation(val message: String) {
    JAPAN_TO_KOREA("일본에서 매입해 한국에서 판매하는 편이 유리합니다."),
    KOREA_TO_JAPAN("한국에서 매입해 일본에서 판매하는 편이 유리합니다."),
    NO_CLEAR_ADVANTAGE("현재 추정으로는 뚜렷하게 유리한 시장이 없습니다.")
}

data class CrossMarketComparison(
    val japanToKoreaMargin: MarginRange,
    val koreaToJapanMargin: MarginRange,
    val recommendation: CrossMarketRecommendation
)

object MarketValueCalculator {
    const val KOREA_FEE_RATE: Double = 0.10
    const val KOREA_LOCAL_SHIPPING_KRW: Long = 5_000L
    const val JAPAN_FEE_RATE: Double = 0.10
    const val JAPAN_LOCAL_SHIPPING_JPY: Long = 1_000L
    const val INTERNATIONAL_SHIPPING_KRW: Long = 30_000L

    fun calculateKoreaNetProceeds(range: KoreaSaleRange): NetProceedsRange {
        val lowNet = (range.low * (1.0 - KOREA_FEE_RATE)).roundToLong() - KOREA_LOCAL_SHIPPING_KRW
        val highNet = (range.high * (1.0 - KOREA_FEE_RATE)).roundToLong() - KOREA_LOCAL_SHIPPING_KRW
        return NetProceedsRange(
            low = max(0L, lowNet),
            high = max(0L, highNet)
        )
    }

    fun calculateJapanNetProceeds(range: JapanSaleRange): NetProceedsRange {
        val lowNet = (range.low * (1.0 - JAPAN_FEE_RATE)).roundToLong() - JAPAN_LOCAL_SHIPPING_JPY
        val highNet = (range.high * (1.0 - JAPAN_FEE_RATE)).roundToLong() - JAPAN_LOCAL_SHIPPING_JPY
        return NetProceedsRange(
            low = max(0L, lowNet),
            high = max(0L, highNet)
        )
    }

    fun calculateCrossMarketComparison(
        koreaRange: KoreaSaleRange,
        japanRange: JapanSaleRange,
        jpyToKrwRate: Double
    ): CrossMarketComparison {
        // Japan purchase -> Korea sale
        val japanPurchaseHighKRW = (japanRange.high * jpyToKrwRate).roundToLong()
        val japanPurchaseLowKRW = (japanRange.low * jpyToKrwRate).roundToLong()

        val j2kCostHigh = japanPurchaseHighKRW + INTERNATIONAL_SHIPPING_KRW
        val j2kCostLow = japanPurchaseLowKRW + INTERNATIONAL_SHIPPING_KRW

        val koreaNetSaleLow = (koreaRange.low * (1.0 - KOREA_FEE_RATE)).roundToLong()
        val koreaNetSaleHigh = (koreaRange.high * (1.0 - KOREA_FEE_RATE)).roundToLong()

        val j2kDiff1 = koreaNetSaleLow - j2kCostHigh
        val j2kDiff2 = koreaNetSaleHigh - j2kCostLow
        val j2kMargin = MarginRange(
            low = min(j2kDiff1, j2kDiff2),
            high = max(j2kDiff1, j2kDiff2)
        )

        // Korea purchase -> Japan sale
        val k2jCostHigh = koreaRange.high + INTERNATIONAL_SHIPPING_KRW
        val k2jCostLow = koreaRange.low + INTERNATIONAL_SHIPPING_KRW

        val japanNetSaleLowKRW = ((japanRange.low * (1.0 - JAPAN_FEE_RATE)) * jpyToKrwRate).roundToLong()
        val japanNetSaleHighKRW = ((japanRange.high * (1.0 - JAPAN_FEE_RATE)) * jpyToKrwRate).roundToLong()

        val k2jDiff1 = japanNetSaleLowKRW - k2jCostHigh
        val k2jDiff2 = japanNetSaleHighKRW - k2jCostLow
        val k2jMargin = MarginRange(
            low = min(k2jDiff1, k2jDiff2),
            high = max(k2jDiff1, k2jDiff2)
        )

        // Recommendation
        val j2kMid = (j2kMargin.low + j2kMargin.high) / 2.0
        val k2jMid = (k2jMargin.low + k2jMargin.high) / 2.0

        val recommendation = when {
            j2kMid > 0.0 && j2kMid >= k2jMid -> CrossMarketRecommendation.JAPAN_TO_KOREA
            k2jMid > 0.0 -> CrossMarketRecommendation.KOREA_TO_JAPAN
            else -> CrossMarketRecommendation.NO_CLEAR_ADVANTAGE
        }

        return CrossMarketComparison(
            japanToKoreaMargin = j2kMargin,
            koreaToJapanMargin = k2jMargin,
            recommendation = recommendation
        )
    }
}
