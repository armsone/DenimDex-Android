package com.armsone.denimdex.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ports handoff 9.2 #2 (CountdownFormatterTests.swift, 4 cases -> expanded to per-assertion vectors). */
class CountdownFormatterTest {

    @Test
    fun `remainingSeconds counts down from 90`() {
        assertEquals(90, CountdownFormatter.remainingSeconds(0.0))
        assertEquals(80, CountdownFormatter.remainingSeconds(10.0))
        assertEquals(1, CountdownFormatter.remainingSeconds(89.9))
        assertEquals(0, CountdownFormatter.remainingSeconds(90.0))
        assertEquals(0, CountdownFormatter.remainingSeconds(120.0))
    }

    @Test
    fun `formatMinutesSeconds matches mm colon ss`() {
        assertEquals("1:30", CountdownFormatter.formatMinutesSeconds(90))
        assertEquals("0:09", CountdownFormatter.formatMinutesSeconds(9))
        assertEquals("0:00", CountdownFormatter.formatMinutesSeconds(0))
        assertEquals("0:00", CountdownFormatter.formatMinutesSeconds(-5))
    }

    @Test
    fun `progressFraction decreases from 1 to 0`() {
        assertEquals(1.0f, CountdownFormatter.progressFraction(0.0), 0.001f)
        assertEquals(0.0f, CountdownFormatter.progressFraction(90.0), 0.001f)
    }

    @Test
    fun `isExpired flips exactly at 90 seconds`() {
        assertFalse(CountdownFormatter.isExpired(89.999))
        assertTrue(CountdownFormatter.isExpired(90.0))
    }
}
