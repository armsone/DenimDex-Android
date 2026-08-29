package com.armsone.denimdex.core.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ports handoff 9.2 (QuickValuePromptBuilderTests.swift) against the V3 prompt contract. */
class QuickValuePromptBuilderTest {

    @Test
    fun `prompt forbids web search and external tools for bounded response time`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(1)

        assertTrue(prompt.contains("웹 검색, 외부 도구 호출"))
        assertTrue(prompt.contains("즉시 응답"))
    }

    @Test
    fun `prompt enumerates photo identifiers in order`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(3)
        assertTrue(prompt.contains("1번 사진: photo_1"))
        assertTrue(prompt.contains("2번 사진: photo_2"))
        assertTrue(prompt.contains("3번 사진: photo_3"))
        assertFalse(prompt.contains("4번 사진: photo_4"))
    }

    @Test
    fun `prompt declares schemaVersion 3 and quick_value task`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(1)
        assertTrue(prompt.contains("\"schemaVersion\": 3"))
        assertTrue(prompt.contains("\"task\": \"quick_value\""))
    }

    @Test
    fun `prompt requires fair purchase and sale ranges for korea and japan and exchange rate`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(5)
        assertTrue(prompt.contains("koreaFairPurchaseRange"))
        assertTrue(prompt.contains("japanFairPurchaseRange"))
        assertTrue(prompt.contains("koreaSaleRange"))
        assertTrue(prompt.contains("japanSaleRange"))
        assertTrue(prompt.contains("jpyToKrwRate"))
    }

    @Test
    fun `prompt requires variant, rarityLevel, raritySummary, and rarityReasons`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(2)
        assertTrue(prompt.contains("\"variant\": \"string\""))
        assertTrue(prompt.contains("\"estimatedProductionYear\": \"string\""))
        assertTrue(prompt.contains("\"estimatedFactory\": \"string\""))
        assertTrue(prompt.contains("\"rarityLevel\": \"unknown | common | uncommon | rare | extremely_rare\""))
        assertTrue(prompt.contains("\"raritySummary\": \"string\""))
        assertTrue(prompt.contains("\"rarityReasons\": [\"string\"]"))
    }

    @Test
    fun `prompt states evidence poor rarity is conservative and distinguishes fair purchase from sale price`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(2)
        assertTrue(prompt.contains("적정 매입가(fairPurchaseRange)와 예상 판매가(saleRange)를 구분하여"))
        assertTrue(prompt.contains("근거가 부족한 경우 희귀도(rarityLevel)는 보수적으로 낮게 추정하고"))
    }

    @Test
    fun `prompt is bounded to the 20 photo AIBI transfer maximum`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(30)
        assertTrue(prompt.contains("20번 사진: photo_20"))
        assertFalse(prompt.contains("21번 사진: photo_21"))
    }

    @Test
    fun `prompt clamps a non-positive count up to 1 photo`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(0)
        assertTrue(prompt.contains("1번 사진: photo_1"))
        assertFalse(prompt.contains("2번 사진: photo_2"))
    }
}
