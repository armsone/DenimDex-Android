package com.armsone.denimdex.feature.scan

import com.armsone.denimdex.core.aibi.AIBIPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickValueCountdownTrackerTest {

    @Test
    fun `isCountdownEligiblePhase returns true only for GENERATING and STABILIZING`() {
        // Submission-confirmed generation phases
        assertTrue(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.GENERATING))
        assertTrue(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.STABILIZING))

        // Pre-generation phases must not start countdown
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.IDLE))
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.INITIALIZING))
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.NAVIGATING))
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.READY_CHECKING))
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.ATTACHING_MEDIA))
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.INJECTING_PROMPT))
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.SUBMITTING))

        // Terminal / Fallback phases
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.COMPLETED))
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.FALLBACK_REQUIRED))
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.FAILED))
        assertFalse(QuickValueCountdownTracker.isCountdownEligiblePhase(AIBIPhase.CANCELLED))
    }

    @Test
    fun `QuickValueRunner companion delegates to isCountdownEligiblePhase`() {
        assertTrue(QuickValueRunner.isCountdownEligiblePhase(AIBIPhase.GENERATING))
        assertTrue(QuickValueRunner.isCountdownEligiblePhase(AIBIPhase.STABILIZING))
        assertFalse(QuickValueRunner.isCountdownEligiblePhase(AIBIPhase.ATTACHING_MEDIA))
        assertFalse(QuickValueRunner.isCountdownEligiblePhase(AIBIPhase.SUBMITTING))
    }

    @Test
    fun `countdown start timestamp remains unset during all pre-generation phases`() {
        var currentTime = 1_000_000L
        val tracker = QuickValueCountdownTracker(clock = { currentTime })

        val prePhases = listOf(
            AIBIPhase.INITIALIZING,
            AIBIPhase.NAVIGATING,
            AIBIPhase.READY_CHECKING,
            AIBIPhase.ATTACHING_MEDIA,
            AIBIPhase.INJECTING_PROMPT,
            AIBIPhase.SUBMITTING
        )

        for (phase in prePhases) {
            currentTime += 10_000L // +10s per phase
            val status = tracker.onPhaseProgress(phase, nowMs = currentTime)
            assertEquals(CountdownStatus.NotStarted, status)
            assertNull(tracker.countdownStartTimeMs)
            assertFalse(tracker.isStarted)
            assertNull(tracker.computeCurrentElapsed(nowMs = currentTime))
            assertFalse(tracker.isExpired(nowMs = currentTime))
        }

        // Even after 100s in pre-generation, expiry is not triggered
        currentTime += 100_000L
        assertFalse(tracker.isExpired(nowMs = currentTime))
        assertNull(tracker.computeCurrentElapsed(nowMs = currentTime))
    }

    @Test
    fun `starts exactly once on first GENERATING progress event and never resets on later ticks`() {
        var currentTime = 5_000L
        val tracker = QuickValueCountdownTracker(clock = { currentTime })

        // Pre-generation
        assertEquals(CountdownStatus.NotStarted, tracker.onPhaseProgress(AIBIPhase.ATTACHING_MEDIA, nowMs = currentTime))
        assertNull(tracker.countdownStartTimeMs)

        // First GENERATING event at t=15_000ms
        currentTime = 15_000L
        val firstGenStatus = tracker.onPhaseProgress(AIBIPhase.GENERATING, nowMs = currentTime)
        assertTrue(firstGenStatus is CountdownStatus.Active)
        assertEquals(0.0, (firstGenStatus as CountdownStatus.Active).elapsedSeconds, 0.001)
        assertEquals(15_000L, tracker.countdownStartTimeMs)
        assertTrue(tracker.isStarted)

        // Subsequent GENERATING tick at t=25_000ms (+10s)
        currentTime = 25_000L
        val secondGenStatus = tracker.onPhaseProgress(AIBIPhase.GENERATING, nowMs = currentTime)
        assertTrue(secondGenStatus is CountdownStatus.Active)
        assertEquals(10.0, (secondGenStatus as CountdownStatus.Active).elapsedSeconds, 0.001)
        assertEquals(15_000L, tracker.countdownStartTimeMs) // Must remain 15_000L

        // Later STABILIZING tick at t=35_000ms (+20s from generation start)
        currentTime = 35_000L
        val stabilizingStatus = tracker.onPhaseProgress(AIBIPhase.STABILIZING, nowMs = currentTime)
        assertTrue(stabilizingStatus is CountdownStatus.Active)
        assertEquals(20.0, (stabilizingStatus as CountdownStatus.Active).elapsedSeconds, 0.001)
        assertEquals(15_000L, tracker.countdownStartTimeMs) // Must remain 15_000L

        // Another GENERATING tick at t=45_000ms (+30s)
        currentTime = 45_000L
        val thirdGenStatus = tracker.onPhaseProgress(AIBIPhase.GENERATING, nowMs = currentTime)
        assertTrue(thirdGenStatus is CountdownStatus.Active)
        assertEquals(30.0, (thirdGenStatus as CountdownStatus.Active).elapsedSeconds, 0.001)
        assertEquals(15_000L, tracker.countdownStartTimeMs) // Still unchanged
    }

    @Test
    fun `starts on STABILIZING if received as first submission-confirmed progress event`() {
        val tracker = QuickValueCountdownTracker()
        val status = tracker.onPhaseProgress(AIBIPhase.STABILIZING, nowMs = 20_000L)

        assertTrue(status is CountdownStatus.Active)
        assertEquals(0.0, (status as CountdownStatus.Active).elapsedSeconds, 0.001)
        assertEquals(20_000L, tracker.countdownStartTimeMs)
    }

    @Test
    fun `preserves fixed 90-second generation window regardless of attachment duration`() {
        var currentTime = 0L
        val tracker = QuickValueCountdownTracker(clock = { currentTime })

        // Pre-generation (photo dedup, normalizer, attachment) takes 55 seconds
        tracker.onPhaseProgress(AIBIPhase.INITIALIZING, nowMs = currentTime)
        currentTime = 55_000L
        tracker.onPhaseProgress(AIBIPhase.ATTACHING_MEDIA, nowMs = currentTime)
        assertNull(tracker.countdownStartTimeMs)

        // Submission confirmed and generation begins at t=60_000ms
        currentTime = 60_000L
        tracker.onPhaseProgress(AIBIPhase.GENERATING, nowMs = currentTime)
        assertEquals(60_000L, tracker.countdownStartTimeMs)

        // At t=90_000ms (90s from run start, 30s into generation) -> still active with 30s elapsed (60s remaining)
        currentTime = 90_000L
        assertEquals(30.0, tracker.computeCurrentElapsed(nowMs = currentTime)!!, 0.001)
        assertFalse(tracker.isExpired(nowMs = currentTime))
        val midStatus = tracker.onPhaseProgress(AIBIPhase.GENERATING, nowMs = currentTime)
        assertTrue(midStatus is CountdownStatus.Active)
        assertEquals(30.0, (midStatus as CountdownStatus.Active).elapsedSeconds, 0.001)

        // At t=149_900ms (89.9s into generation) -> still active
        currentTime = 149_900L
        assertEquals(89.9, tracker.computeCurrentElapsed(nowMs = currentTime)!!, 0.001)
        assertFalse(tracker.isExpired(nowMs = currentTime))

        // At t=150_000ms (90.0s into generation) -> expired!
        currentTime = 150_000L
        assertEquals(90.0, tracker.computeCurrentElapsed(nowMs = currentTime)!!, 0.001)
        assertTrue(tracker.isExpired(nowMs = currentTime))
        val expiredStatus = tracker.onPhaseProgress(AIBIPhase.GENERATING, nowMs = currentTime)
        assertTrue(expiredStatus is CountdownStatus.Expired)
        assertEquals(90.0, (expiredStatus as CountdownStatus.Expired).elapsedSeconds, 0.001)
    }

    @Test
    fun `reset clears countdown timestamp and enables clean subsequent runs`() {
        var currentTime = 10_000L
        val tracker = QuickValueCountdownTracker(clock = { currentTime })

        // Run 1: starts generation at t=10_000ms
        tracker.onPhaseProgress(AIBIPhase.GENERATING, nowMs = currentTime)
        assertEquals(10_000L, tracker.countdownStartTimeMs)
        assertTrue(tracker.isStarted)

        // Advance 30s
        currentTime = 40_000L
        assertEquals(30.0, tracker.computeCurrentElapsed(nowMs = currentTime)!!, 0.001)

        // User cancels / resets
        tracker.reset()
        assertNull(tracker.countdownStartTimeMs)
        assertFalse(tracker.isStarted)
        assertNull(tracker.computeCurrentElapsed(nowMs = currentTime))
        assertFalse(tracker.isExpired(nowMs = currentTime))

        // Run 2: starts pre-generation at t=50_000ms, generation at t=70_000ms
        currentTime = 50_000L
        tracker.onPhaseProgress(AIBIPhase.NAVIGATING, nowMs = currentTime)
        assertNull(tracker.countdownStartTimeMs)

        currentTime = 70_000L
        val run2GenStatus = tracker.onPhaseProgress(AIBIPhase.GENERATING, nowMs = currentTime)
        assertTrue(run2GenStatus is CountdownStatus.Active)
        assertEquals(0.0, (run2GenStatus as CountdownStatus.Active).elapsedSeconds, 0.001)
        assertEquals(70_000L, tracker.countdownStartTimeMs)

        // At t=85_000ms, elapsed is 15.0s
        currentTime = 85_000L
        assertEquals(15.0, tracker.computeCurrentElapsed(nowMs = currentTime)!!, 0.001)
    }
}
