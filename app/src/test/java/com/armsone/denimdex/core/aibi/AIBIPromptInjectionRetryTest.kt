package com.armsone.denimdex.core.aibi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIBIPromptInjectionRetryTest {

    @Test
    fun `classify returns SUCCESS_VERIFIED when inject succeeds and verify reports exact trimmed match`() {
        val injectResult = AIBIInjectionAttemptResult(
            isSuccess = true,
            isContentEditable = true,
            injectedLength = 120
        )
        val verifyResult = AIBIVerifyPromptResult(
            isSuccess = true,
            matches = true,
            currentLength = 120
        )
        val outcome = AIBIPromptInjectionClassifier.classify(injectResult, verifyResult)
        assertEquals(AIBIPromptInjectionOutcome.SUCCESS_VERIFIED, outcome)
        assertFalse(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 1))
        assertFalse(AIBIPromptInjectionClassifier.isTerminal(outcome))
    }

    @Test
    fun `classify returns EXISTING_TEXT_PRESERVED when inject returns EXISTING_TEXT_PRESERVED`() {
        val injectResult = AIBIInjectionAttemptResult(
            isSuccess = false,
            errorCode = "EXISTING_TEXT_PRESERVED"
        )
        val outcome = AIBIPromptInjectionClassifier.classify(injectResult, verifyResult = null)
        assertEquals(AIBIPromptInjectionOutcome.EXISTING_TEXT_PRESERVED, outcome)
        // Must never retry: terminal outcome to avoid overwriting user text
        assertFalse(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 1))
        assertFalse(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 2))
        assertFalse(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 3))
        assertFalse(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 4))
        assertTrue(AIBIPromptInjectionClassifier.isTerminal(outcome))
    }

    @Test
    fun `classify returns RETRYABLE_MISSING_INPUT when inject returns INPUT_NOT_FOUND`() {
        val injectResult = AIBIInjectionAttemptResult(
            isSuccess = false,
            errorCode = "INPUT_NOT_FOUND"
        )
        val outcome = AIBIPromptInjectionClassifier.classify(injectResult, verifyResult = null)
        assertEquals(AIBIPromptInjectionOutcome.RETRYABLE_MISSING_INPUT, outcome)
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 1))
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 2))
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 3))
        assertFalse(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 4))
        assertFalse(AIBIPromptInjectionClassifier.isTerminal(outcome))
    }

    @Test
    fun `classify returns RETRYABLE_VERIFICATION_MISMATCH when inject succeeds but verify reports isMatched false`() {
        val injectResult = AIBIInjectionAttemptResult(
            isSuccess = true,
            isContentEditable = true,
            injectedLength = 85
        )
        val verifyResult = AIBIVerifyPromptResult(
            isSuccess = true,
            matches = false,
            currentLength = 12
        )
        val outcome = AIBIPromptInjectionClassifier.classify(injectResult, verifyResult)
        assertEquals(AIBIPromptInjectionOutcome.RETRYABLE_VERIFICATION_MISMATCH, outcome)
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 1))
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 3))
        assertFalse(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 4))
        assertFalse(AIBIPromptInjectionClassifier.isTerminal(outcome))
    }

    @Test
    fun `classify returns RETRYABLE_VERIFICATION_MISMATCH when composer is cleared after attachment or hydration`() {
        val injectResult = AIBIInjectionAttemptResult(
            isSuccess = true,
            isContentEditable = true,
            injectedLength = 200
        )
        val verifyResult = AIBIVerifyPromptResult(
            isSuccess = true,
            matches = false,
            currentLength = 0
        )
        val outcome = AIBIPromptInjectionClassifier.classify(injectResult, verifyResult)
        assertEquals(AIBIPromptInjectionOutcome.RETRYABLE_VERIFICATION_MISMATCH, outcome)
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 1))
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 2))
        assertFalse(AIBIPromptInjectionClassifier.isTerminal(outcome))
    }

    @Test
    fun `classify returns RETRYABLE_MISSING_INPUT when verify returns INPUT_NOT_FOUND`() {
        val injectResult = AIBIInjectionAttemptResult(
            isSuccess = true,
            isContentEditable = true,
            injectedLength = 100
        )
        val verifyResult = AIBIVerifyPromptResult(
            isSuccess = false,
            matches = false,
            currentLength = 0,
            errorCode = "INPUT_NOT_FOUND"
        )
        val outcome = AIBIPromptInjectionClassifier.classify(injectResult, verifyResult)
        assertEquals(AIBIPromptInjectionOutcome.RETRYABLE_MISSING_INPUT, outcome)
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 1))
        assertFalse(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 4))
    }

    @Test
    fun `classify returns RETRYABLE_EVALUATION_FAILED when inject evaluation returns null or malformed JSON`() {
        val evalFailed = AIBIPromptInjectionClassifier.parseInjectionResult(null)
        val outcome1 = AIBIPromptInjectionClassifier.classify(evalFailed, verifyResult = null)
        assertEquals(AIBIPromptInjectionOutcome.RETRYABLE_EVALUATION_FAILED, outcome1)
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome1, currentAttempt = 1))

        val malformed = AIBIPromptInjectionClassifier.parseInjectionResult("NOT_JSON")
        val outcome2 = AIBIPromptInjectionClassifier.classify(malformed, verifyResult = null)
        assertEquals(AIBIPromptInjectionOutcome.RETRYABLE_EVALUATION_FAILED, outcome2)
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome2, currentAttempt = 2))
    }

    @Test
    fun `classify returns RETRYABLE_EVALUATION_FAILED when verify evaluation returns null or malformed JSON`() {
        val injectSuccess = AIBIInjectionAttemptResult(isSuccess = true, injectedLength = 50)
        val verifyNull = AIBIPromptInjectionClassifier.parseVerifyResult(null)
        val outcome = AIBIPromptInjectionClassifier.classify(injectSuccess, verifyNull)
        assertEquals(AIBIPromptInjectionOutcome.RETRYABLE_EVALUATION_FAILED, outcome)
        assertTrue(AIBIPromptInjectionClassifier.shouldRetry(outcome, currentAttempt = 1))
    }

    @Test
    fun `parseInjectionResult correctly parses successful and error responses`() {
        val successJson = """{"success":true,"data":{"injectedLength":95,"isContentEditable":true}}"""
        val parsedSuccess = AIBIPromptInjectionClassifier.parseInjectionResult(successJson)
        assertTrue(parsedSuccess.isSuccess)
        assertEquals(95, parsedSuccess.injectedLength)
        assertTrue(parsedSuccess.isContentEditable)
        assertNull(parsedSuccess.errorCode)

        val preservedJson = """{"success":false,"code":"EXISTING_TEXT_PRESERVED","error":"User text preserved"}"""
        val parsedPreserved = AIBIPromptInjectionClassifier.parseInjectionResult(preservedJson)
        assertFalse(parsedPreserved.isSuccess)
        assertEquals("EXISTING_TEXT_PRESERVED", parsedPreserved.errorCode)

        val notFoundJson = """{"success":false,"code":"INPUT_NOT_FOUND","error":"Element not found"}"""
        val parsedNotFound = AIBIPromptInjectionClassifier.parseInjectionResult(notFoundJson)
        assertFalse(parsedNotFound.isSuccess)
        assertEquals("INPUT_NOT_FOUND", parsedNotFound.errorCode)
    }

    @Test
    fun `parseVerifyResult correctly parses match without returning prompt text`() {
        val matchedJson = """{"success":true,"data":{"matches":true,"currentLength":150,"isContentEditable":true}}"""
        val parsedMatched = AIBIPromptInjectionClassifier.parseVerifyResult(matchedJson)
        assertTrue(parsedMatched.isSuccess)
        assertTrue(parsedMatched.matches)
        assertEquals(150, parsedMatched.currentLength)
        assertNull(parsedMatched.errorCode)

        val mismatchedJson = """{"success":true,"data":{"matches":false,"currentLength":0,"isContentEditable":false}}"""
        val parsedMismatched = AIBIPromptInjectionClassifier.parseVerifyResult(mismatchedJson)
        assertTrue(parsedMismatched.isSuccess)
        assertFalse(parsedMismatched.matches)
        assertEquals(0, parsedMismatched.currentLength)
        assertNull(parsedMismatched.errorCode)
    }

    @Test
    fun `parseVerifyResult handles empty, null, and malformed inputs safely`() {
        val nullResult = AIBIPromptInjectionClassifier.parseVerifyResult(null)
        assertFalse(nullResult.isSuccess)
        assertFalse(nullResult.matches)
        assertEquals("EVALUATION_FAILED", nullResult.errorCode)

        val emptyResult = AIBIPromptInjectionClassifier.parseVerifyResult("")
        assertFalse(emptyResult.isSuccess)
        assertEquals("EVALUATION_FAILED", emptyResult.errorCode)

        val malformedResult = AIBIPromptInjectionClassifier.parseVerifyResult("{invalid json}")
        assertFalse(malformedResult.isSuccess)
        assertEquals("MALFORMED_JSON", malformedResult.errorCode)
    }

    @Test
    fun `timingProfile default specifies 4 max attempts and 600ms retry delay`() {
        val profile = AIBITimingProfile.default
        assertEquals(4, profile.promptInjectionMaxAttempts)
        assertEquals(600L, profile.promptInjectionRetryDelayMs)
        assertEquals(AIBIPromptInjectionClassifier.DEFAULT_MAX_ATTEMPTS, profile.promptInjectionMaxAttempts)
        assertEquals(AIBIPromptInjectionClassifier.DEFAULT_RETRY_DELAY_MS, profile.promptInjectionRetryDelayMs)
    }
}
