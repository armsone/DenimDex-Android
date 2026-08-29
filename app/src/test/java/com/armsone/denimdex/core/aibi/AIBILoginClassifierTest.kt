package com.armsone.denimdex.core.aibi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIBILoginClassifierTest {

    @Test
    fun `classify returns LOGGED_IN when authenticated marker is present`() {
        val result = AIBILoginClassifier.classify(
            hasAuthenticatedMarker = true,
            hasLoginMarker = false,
            hasChallengeMarker = false
        )
        assertEquals(LoginStatus.LOGGED_IN, result)
    }

    @Test
    fun `classify returns LOGIN_REQUIRED when login marker is present and auth marker is absent`() {
        val result = AIBILoginClassifier.classify(
            hasAuthenticatedMarker = false,
            hasLoginMarker = true,
            hasChallengeMarker = false
        )
        assertEquals(LoginStatus.LOGIN_REQUIRED, result)
    }

    @Test
    fun `classify returns UNKNOWN when challenge marker is present`() {
        val result = AIBILoginClassifier.classify(
            hasAuthenticatedMarker = false,
            hasLoginMarker = false,
            hasChallengeMarker = true
        )
        assertEquals(LoginStatus.UNKNOWN, result)
    }

    @Test
    fun `classify returns UNKNOWN when neither auth nor login nor challenge marker is present`() {
        val result = AIBILoginClassifier.classify(
            hasAuthenticatedMarker = false,
            hasLoginMarker = false,
            hasChallengeMarker = false
        )
        assertEquals(LoginStatus.UNKNOWN, result)
    }

    @Test
    fun `classify prioritizes positive authenticated marker over login marker during state transition`() {
        val result = AIBILoginClassifier.classify(
            hasAuthenticatedMarker = true,
            hasLoginMarker = true,
            hasChallengeMarker = false
        )
        assertEquals(LoginStatus.LOGGED_IN, result)
    }

    @Test
    fun `parseProbeResult correctly parses uppercase string values`() {
        assertEquals(LoginStatus.LOGGED_IN, AIBILoginClassifier.parseProbeResult("LOGGED_IN"))
        assertEquals(LoginStatus.LOGIN_REQUIRED, AIBILoginClassifier.parseProbeResult("LOGIN_REQUIRED"))
        assertEquals(LoginStatus.UNKNOWN, AIBILoginClassifier.parseProbeResult("UNKNOWN"))
    }

    @Test
    fun `parseProbeResult handles quotes and whitespace from evaluateJavascript`() {
        assertEquals(LoginStatus.LOGGED_IN, AIBILoginClassifier.parseProbeResult("\"LOGGED_IN\""))
        assertEquals(LoginStatus.LOGIN_REQUIRED, AIBILoginClassifier.parseProbeResult(" \"LOGIN_REQUIRED\" \n"))
        assertEquals(LoginStatus.LOGGED_IN, AIBILoginClassifier.parseProbeResult("logged_in"))
    }

    @Test
    fun `parseProbeResult returns UNKNOWN for unexpected, null, empty or intermediate results`() {
        assertEquals(LoginStatus.UNKNOWN, AIBILoginClassifier.parseProbeResult(null))
        assertEquals(LoginStatus.UNKNOWN, AIBILoginClassifier.parseProbeResult(""))
        assertEquals(LoginStatus.UNKNOWN, AIBILoginClassifier.parseProbeResult("null"))
        assertEquals(LoginStatus.UNKNOWN, AIBILoginClassifier.parseProbeResult("\"NOT_YET\""))
        assertEquals(LoginStatus.UNKNOWN, AIBILoginClassifier.parseProbeResult("undefined"))
    }

    @Test
    fun `shouldBlockScanStart blocks on confirmed LOGIN_REQUIRED or explicit logout only`() {
        // Explicit logout always blocks regardless of probe status
        assertTrue(AIBILoginClassifier.shouldBlockScanStart(LoginStatus.LOGGED_IN, explicitLogout = true))
        assertTrue(AIBILoginClassifier.shouldBlockScanStart(LoginStatus.LOGIN_REQUIRED, explicitLogout = true))
        assertTrue(AIBILoginClassifier.shouldBlockScanStart(LoginStatus.UNKNOWN, explicitLogout = true))
        assertTrue(AIBILoginClassifier.shouldBlockScanStart(LoginStatus.CHECKING, explicitLogout = true))

        // Confirmed LOGIN_REQUIRED blocks when explicitLogout is false
        assertTrue(AIBILoginClassifier.shouldBlockScanStart(LoginStatus.LOGIN_REQUIRED, explicitLogout = false))

        // UNKNOWN, LOGGED_IN, and CHECKING do not block scan start (allowing visible takeover if needed)
        assertFalse(AIBILoginClassifier.shouldBlockScanStart(LoginStatus.UNKNOWN, explicitLogout = false))
        assertFalse(AIBILoginClassifier.shouldBlockScanStart(LoginStatus.LOGGED_IN, explicitLogout = false))
        assertFalse(AIBILoginClassifier.shouldBlockScanStart(LoginStatus.CHECKING, explicitLogout = false))
    }

    @Test
    fun `buildProbeScript includes positive auth selectors and login selectors`() {
        val script = AIBILoginClassifier.buildProbeScript()
        assertTrue(script.contains("data-testid='profile-button'"))
        assertTrue(script.contains("data-testid='user-menu-button'"))
        assertTrue(script.contains("data-testid='login-button'"))
        assertTrue(script.contains("/auth/login"))
        assertTrue(script.contains("LOGGED_IN"))
        assertTrue(script.contains("LOGIN_REQUIRED"))
        assertTrue(script.contains("UNKNOWN"))
        assertTrue(script.contains("open-sidebar-button"))
        assertTrue(script.contains("accounts-profile-button"))
        assertTrue(script.contains("querySelectorAll"))
    }

    @Test
    fun `canInspectUrl accepts only ChatGPT script origins`() {
        assertTrue(AIBILoginClassifier.canInspectUrl("https://chatgpt.com/"))
        assertTrue(AIBILoginClassifier.canInspectUrl("https://foo.chatgpt.com/path"))
        assertFalse(AIBILoginClassifier.canInspectUrl("https://auth.openai.com/authorize"))
        assertFalse(AIBILoginClassifier.canInspectUrl("https://chatgpt.com.evil.example/"))
        assertFalse(AIBILoginClassifier.canInspectUrl(null))
    }
}
