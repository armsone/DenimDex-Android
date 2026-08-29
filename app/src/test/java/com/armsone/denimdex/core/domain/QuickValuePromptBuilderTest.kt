package com.armsone.denimdex.core.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ports handoff 9.2 (QuickValuePromptBuilderTests.swift, 5 cases) against the V2 dual-market prompt contract (4.3). */
class QuickValuePromptBuilderTest {

    @Test
    fun `prompt enumerates photo identifiers in order`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(3)
        assertTrue(prompt.contains("1번 사진: photo_1"))
        assertTrue(prompt.contains("2번 사진: photo_2"))
        assertTrue(prompt.contains("3번 사진: photo_3"))
        assertFalse(prompt.contains("4번 사진: photo_4"))
    }

    @Test
    fun `prompt declares schemaVersion 2 and quick_value task`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(1)
        assertTrue(prompt.contains("\"schemaVersion\": 2"))
        assertTrue(prompt.contains("\"task\": \"quick_value\""))
    }

    @Test
    fun `prompt requires both korea and japan sale ranges and exchange rate`() {
        val prompt = QuickValuePromptBuilder.buildPrompt(5)
        assertTrue(prompt.contains("koreaSaleRange"))
        assertTrue(prompt.contains("japanSaleRange"))
        assertTrue(prompt.contains("jpyToKrwRate"))
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
