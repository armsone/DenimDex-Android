package com.armsone.denimdex.core.model

enum class VerificationState(val rawValue: String, val displayName: String) {
    AI_ESTIMATE("ai_estimate", "AI 추정"),
    USER_CONFIRMED("user_confirmed", "사용자 확인"),
    SOURCE_VERIFIED("source_verified", "출처 확인됨");

    companion object {
        fun fromString(value: String): VerificationState =
            entries.find { it.rawValue.equals(value.trim(), ignoreCase = true) } ?: AI_ESTIMATE
    }
}
