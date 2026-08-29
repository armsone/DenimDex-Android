package com.armsone.denimdex.core.aibi

import org.json.JSONObject
import java.util.UUID

enum class AIBIPhase(val value: String) {
    IDLE("IDLE"),
    INITIALIZING("INITIALIZING"),
    NAVIGATING("NAVIGATING"),
    READY_CHECKING("READY_CHECKING"),
    ATTACHING_MEDIA("ATTACHING_MEDIA"),
    INJECTING_PROMPT("INJECTING_PROMPT"),
    SUBMITTING("SUBMITTING"),
    GENERATING("GENERATING"),
    STABILIZING("STABILIZING"),
    COMPLETED("COMPLETED"),
    FALLBACK_REQUIRED("FALLBACK_REQUIRED"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED")
}

enum class AIBIFallbackReason(val value: String) {
    AUTH_REQUIRED("AUTH_REQUIRED"),
    SECURITY_CHALLENGE_PRESENTED("SECURITY_CHALLENGE_PRESENTED"),
    NAVIGATION_DISALLOWED("NAVIGATION_DISALLOWED"),
    INPUT_NOT_FOUND("INPUT_NOT_FOUND"),
    ATTACHMENT_FAILED("ATTACHMENT_FAILED"),
    READINESS_TIMEOUT("READINESS_TIMEOUT"),
    USER_INTERVENTION_REQUESTED("USER_INTERVENTION_REQUESTED")
}

enum class AIBIPresentationPreference(val value: String) {
    ALWAYS_VISIBLE("ALWAYS_VISIBLE"),
    VISIBLE_WHEN_NEEDED("VISIBLE_WHEN_NEEDED")
}

data class AIBITask(
    val id: UUID = UUID.randomUUID(),
    val providerId: String,
    val promptText: String,
    val attachments: List<AIBIMediaAttachment> = emptyList(),
    val presentation: AIBIPresentationPreference = AIBIPresentationPreference.VISIBLE_WHEN_NEEDED,
    val forceFill: Boolean = false
)

data class AIBIResult(
    val taskId: UUID,
    val providerId: String,
    val rawText: String,
    val cleanedText: String,
    val isComplete: Boolean
)

data class AIBIProgress(
    val phase: AIBIPhase,
    val elapsedSeconds: Double,
    val statusMessage: String,
    val isWaiting: Boolean
) {
    companion object {
        val initial = AIBIProgress(
            phase = AIBIPhase.IDLE,
            elapsedSeconds = 0.0,
            statusMessage = "Ready",
            isWaiting = false
        )
    }
}

interface AIBIResultSink {
    fun commitResult(result: AIBIResult): Result<Unit>
}

data class AIBIProviderSelectors(
    val promptInput: List<String>,
    val submitButton: List<String>,
    val stopButton: List<String>,
    val assistantMessage: List<String>,
    val preCode: List<String>? = null,
    val errorBanner: List<String>,
    val loginIndicator: List<String>,
    val challengeIndicator: List<String>,
    val attachmentInput: List<String> = emptyList(),
    val attachmentTrigger: List<String> = emptyList(),
    val attachmentMenuAction: List<String> = emptyList(),
    val attachmentMenuActionText: List<String> = emptyList(),
    val attachmentPreview: List<String> = emptyList()
)

data class AIBIMediaCapabilities(
    val supportsImages: Boolean = false,
    val maxImagesPerTask: Int = 0,
    val requiresMultipleInputForBatch: Boolean = true
)

data class AIBIProviderConfig(
    val id: String,
    val displayName: String,
    val initialUrl: String,
    val allowedScriptOrigins: List<String>,
    val allowedAuthOrigins: List<String>,
    val selectors: AIBIProviderSelectors,
    val mediaCapabilities: AIBIMediaCapabilities = AIBIMediaCapabilities()
)

data class AIBITimingProfile(
    val readinessTimeoutMs: Long = 35_000L,
    val readinessCadenceMs: Long = 700L,
    val maxReadinessMisses: Int = 12,
    val attachmentTimeoutMs: Long = 30_000L,
    val attachmentCadenceMs: Long = 350L,
    val submitTimeoutMs: Long = 15_000L,
    val submitCadenceMs: Long = 500L,
    val submitVerificationDelayMs: Long = 700L,
    val visibleAutoFillTimeoutMs: Long = 45_000L,
    val observationCadenceMs: Long = 700L,
    val stabilityRequiredTicks: Int = 2,
    val promptInjectionMaxAttempts: Int = 4,
    val promptInjectionRetryDelayMs: Long = 600L
) {
    companion object {
        val default = AIBITimingProfile()
    }
}

enum class AIBIPromptInjectionOutcome {
    SUCCESS_VERIFIED,
    EXISTING_TEXT_PRESERVED,
    RETRYABLE_MISSING_INPUT,
    RETRYABLE_VERIFICATION_MISMATCH,
    RETRYABLE_EVALUATION_FAILED
}

data class AIBIInjectionAttemptResult(
    val isSuccess: Boolean,
    val errorCode: String? = null,
    val isContentEditable: Boolean = false,
    val injectedLength: Int = 0
)

data class AIBIVerifyPromptResult(
    val isSuccess: Boolean,
    val matches: Boolean,
    val currentLength: Int,
    val errorCode: String? = null
)

object AIBIPromptInjectionClassifier {
    const val DEFAULT_MAX_ATTEMPTS = 4
    const val DEFAULT_RETRY_DELAY_MS = 600L

    fun parseInjectionResult(rawJson: String?): AIBIInjectionAttemptResult {
        if (rawJson.isNullOrBlank()) {
            return AIBIInjectionAttemptResult(isSuccess = false, errorCode = "EVALUATION_FAILED")
        }
        return try {
            val json = JSONObject(rawJson)
            val success = json.optBoolean("success", false)
            if (success) {
                val data = json.optJSONObject("data")
                AIBIInjectionAttemptResult(
                    isSuccess = true,
                    isContentEditable = data?.optBoolean("isContentEditable", false) ?: false,
                    injectedLength = data?.optInt("injectedLength", 0) ?: 0
                )
            } else {
                val code = json.optString("code").takeIf { it.isNotEmpty() } ?: "UNKNOWN_ERROR"
                AIBIInjectionAttemptResult(isSuccess = false, errorCode = code)
            }
        } catch (_: Exception) {
            AIBIInjectionAttemptResult(isSuccess = false, errorCode = "MALFORMED_JSON")
        }
    }

    fun parseVerifyResult(rawJson: String?): AIBIVerifyPromptResult {
        if (rawJson.isNullOrBlank()) {
            return AIBIVerifyPromptResult(
                isSuccess = false,
                matches = false,
                currentLength = 0,
                errorCode = "EVALUATION_FAILED"
            )
        }
        return try {
            val json = JSONObject(rawJson)
            val success = json.optBoolean("success", false)
            if (success) {
                val data = json.optJSONObject("data")
                val matches = data?.optBoolean("matches", false) ?: false
                val currentLength = data?.optInt("currentLength", 0) ?: 0
                AIBIVerifyPromptResult(
                    isSuccess = true,
                    matches = matches,
                    currentLength = currentLength
                )
            } else {
                val code = json.optString("code").takeIf { it.isNotEmpty() } ?: "UNKNOWN_ERROR"
                AIBIVerifyPromptResult(
                    isSuccess = false,
                    matches = false,
                    currentLength = 0,
                    errorCode = code
                )
            }
        } catch (_: Exception) {
            AIBIVerifyPromptResult(
                isSuccess = false,
                matches = false,
                currentLength = 0,
                errorCode = "MALFORMED_JSON"
            )
        }
    }

    fun classify(
        injectResult: AIBIInjectionAttemptResult,
        verifyResult: AIBIVerifyPromptResult?
    ): AIBIPromptInjectionOutcome {
        if (!injectResult.isSuccess) {
            return when (injectResult.errorCode) {
                "EXISTING_TEXT_PRESERVED" -> AIBIPromptInjectionOutcome.EXISTING_TEXT_PRESERVED
                "INPUT_NOT_FOUND" -> AIBIPromptInjectionOutcome.RETRYABLE_MISSING_INPUT
                else -> AIBIPromptInjectionOutcome.RETRYABLE_EVALUATION_FAILED
            }
        }

        if (verifyResult == null || !verifyResult.isSuccess) {
            return if (verifyResult?.errorCode == "INPUT_NOT_FOUND") {
                AIBIPromptInjectionOutcome.RETRYABLE_MISSING_INPUT
            } else {
                AIBIPromptInjectionOutcome.RETRYABLE_EVALUATION_FAILED
            }
        }

        return if (verifyResult.matches) {
            AIBIPromptInjectionOutcome.SUCCESS_VERIFIED
        } else {
            AIBIPromptInjectionOutcome.RETRYABLE_VERIFICATION_MISMATCH
        }
    }

    fun shouldRetry(
        outcome: AIBIPromptInjectionOutcome,
        currentAttempt: Int,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS
    ): Boolean {
        if (outcome == AIBIPromptInjectionOutcome.EXISTING_TEXT_PRESERVED) return false
        if (outcome == AIBIPromptInjectionOutcome.SUCCESS_VERIFIED) return false
        return currentAttempt < maxAttempts
    }

    fun isTerminal(outcome: AIBIPromptInjectionOutcome): Boolean {
        return outcome == AIBIPromptInjectionOutcome.EXISTING_TEXT_PRESERVED
    }
}
