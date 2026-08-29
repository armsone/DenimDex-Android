package com.armsone.denimdex.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Ports handoff 9.2 #? / 4.7 (QuickValueImagePolicyTests.swift, 4 cases: one per count tier). */
class QuickValueImagePolicyTest {

    @Test
    fun `1 to 8 photos use 2048px long edge and quality 84`() {
        val policy = QuickValueImagePolicy.policyForCount(8)
        assertEquals(2048, policy.maxLongEdgePixels)
        assertEquals(84, policy.initialJpegQuality)
    }

    @Test
    fun `9 to 12 photos use 1792px long edge and quality 82`() {
        val policy = QuickValueImagePolicy.policyForCount(12)
        assertEquals(1792, policy.maxLongEdgePixels)
        assertEquals(82, policy.initialJpegQuality)
    }

    @Test
    fun `13 to 16 photos use 1600px long edge and quality 80`() {
        val policy = QuickValueImagePolicy.policyForCount(16)
        assertEquals(1600, policy.maxLongEdgePixels)
        assertEquals(80, policy.initialJpegQuality)
    }

    @Test
    fun `17 to 20 photos use 1536px long edge and quality 78`() {
        val policy = QuickValueImagePolicy.policyForCount(20)
        assertEquals(1536, policy.maxLongEdgePixels)
        assertEquals(78, policy.initialJpegQuality)
    }

    @Test
    fun `capture and send limits match handoff constants`() {
        assertEquals(30, QuickValueImagePolicy.CAPTURE_MAXIMUM_COUNT)
        assertEquals(20, QuickValueImagePolicy.SEND_MAXIMUM_COUNT)
        assertEquals(16_000_000L, QuickValueImagePolicy.TOTAL_BATCH_BUDGET_BYTES)
        assertEquals(2_000_000L, QuickValueImagePolicy.MAXIMUM_BYTES_PER_IMAGE)
    }
}
