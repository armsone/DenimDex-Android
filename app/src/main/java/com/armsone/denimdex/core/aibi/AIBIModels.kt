package com.armsone.denimdex.core.aibi

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
    val stabilityRequiredTicks: Int = 2
) {
    companion object {
        val default = AIBITimingProfile()
    }
}
