package com.armsone.denimdex.core.domain

import com.armsone.denimdex.core.model.JapanSaleRange
import com.armsone.denimdex.core.model.KoreaSaleRange
import org.junit.Assert.assertEquals
import org.junit.Test

/** Ports handoff 9.2 #1 (MarketValueCalculatorTests.swift, 8 cases). */
class MarketValueCalculatorTest {

    @Test
    fun `korea net proceeds deducts 10 percent fee and 5000 KRW shipping`() {
        val net = MarketValueCalculator.calculateKoreaNetProceeds(KoreaSaleRange(low = 80_000, high = 180_000))
        assertEquals(67_000L, net.low)
        assertEquals(157_000L, net.high)
    }

    @Test
    fun `japan net proceeds deducts 10 percent fee and 1000 JPY shipping`() {
        val net = MarketValueCalculator.calculateJapanNetProceeds(JapanSaleRange(low = 8_000, high = 18_000))
        assertEquals(6_200L, net.low)
        assertEquals(15_200L, net.high)
    }

    @Test
    fun `net proceeds are clamped to zero and never negative`() {
        val net = MarketValueCalculator.calculateKoreaNetProceeds(KoreaSaleRange(low = 0, high = 1_000))
        assertEquals(0L, net.low)
        assertEquals(0L, net.high)
    }

    @Test
    fun `cross market margin exposes losses without clamping to zero`() {
        val comparison = MarketValueCalculator.calculateCrossMarketComparison(
            koreaRange = KoreaSaleRange(low = 100_000, high = 200_000),
            japanRange = JapanSaleRange(low = 8_000, high = 16_000),
            jpyToKrwRate = 9.0
        )
        assertEquals(-84_000L, comparison.japanToKoreaMargin.low)
        assertEquals(78_000L, comparison.japanToKoreaMargin.high)
        assertEquals(-165_200L, comparison.koreaToJapanMargin.low)
        assertEquals(-400L, comparison.koreaToJapanMargin.high)
        assertEquals(CrossMarketRecommendation.NO_CLEAR_ADVANTAGE, comparison.recommendation)
    }

    @Test
    fun `recommends japan to korea when that margin is clearly better`() {
        val comparison = MarketValueCalculator.calculateCrossMarketComparison(
            koreaRange = KoreaSaleRange(low = 300_000, high = 400_000),
            japanRange = JapanSaleRange(low = 5_000, high = 8_000),
            jpyToKrwRate = 9.0
        )
        assertEquals(CrossMarketRecommendation.JAPAN_TO_KOREA, comparison.recommendation)
    }

    @Test
    fun `recommends korea to japan when that margin is clearly better`() {
        val comparison = MarketValueCalculator.calculateCrossMarketComparison(
            koreaRange = KoreaSaleRange(low = 10_000, high = 12_000),
            japanRange = JapanSaleRange(low = 50_000, high = 60_000),
            jpyToKrwRate = 1.0
        )
        assertEquals(CrossMarketRecommendation.KOREA_TO_JAPAN, comparison.recommendation)
    }
}
