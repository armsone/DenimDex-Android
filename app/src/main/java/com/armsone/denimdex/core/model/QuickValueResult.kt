package com.armsone.denimdex.core.model

import androidx.compose.ui.graphics.Color
import com.armsone.denimdex.core.design.DenimColors

enum class QuickValueConfidence(val rawValue: String, val displayName: String, val iconName: String, val color: Color) {
    HIGH("high", "신뢰도 높음", "checkmark.seal.fill", DenimColors.successGreen),
    MEDIUM("medium", "신뢰도 보통", "questionmark.circle.fill", DenimColors.brass),
    LOW("low", "신뢰도 낮음", "exclamationmark.triangle.fill", DenimColors.signalRed),
    UNKNOWN("unknown", "판단 보류", "questionmark.diamond.fill", DenimColors.signalRed);

    companion object {
        fun fromString(value: String): QuickValueConfidence =
            entries.find { it.rawValue.equals(value.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

enum class QuickValueCondition(val rawValue: String, val displayName: String) {
    EXCELLENT("excellent", "최상"),
    GOOD("good", "양호"),
    FAIR("fair", "보통"),
    POOR("poor", "사용감 많음"),
    UNKNOWN("unknown", "확인되지 않음");

    companion object {
        fun fromString(value: String): QuickValueCondition =
            entries.find { it.rawValue.equals(value.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

enum class Certainty(val rawValue: String) {
    OBSERVED("observed"),
    REPORTED("reported"),
    INFERRED("inferred");

    companion object {
        fun fromString(value: String): Certainty? =
            entries.find { it.rawValue.equals(value.trim(), ignoreCase = true) }
    }
}

enum class QuickValueBasis(val rawValue: String, val badgeText: String) {
    AI_QUICK_VALUE("ai_quick_value", "AI 가치 추정"),
    DEEP_INSPECT("deep_inspect", "정밀 조사 (준비 중)"),
    USER_RECORD("user_record", "사용자 기록");

    companion object {
        fun fromString(value: String): QuickValueBasis =
            entries.find { it.rawValue.equals(value.trim(), ignoreCase = true) } ?: AI_QUICK_VALUE
    }
}

data class ProductGuess(
    val brand: String = "",
    val model: String = "",
    val era: String = ""
)

data class KoreaSaleRange(
    val low: Long = 0L,
    val high: Long = 0L
)

data class JapanSaleRange(
    val low: Long = 0L,
    val high: Long = 0L
)

data class Observation(
    val feature: String = "",
    val value: String = "",
    val evidencePhotoRole: String = "",
    val certainty: Certainty = Certainty.OBSERVED
)

data class QuickValueResult(
    val schemaVersion: Int = 2,
    val task: String = "quick_value",
    val productGuess: ProductGuess = ProductGuess(),
    val summary: String = "",
    val confidence: QuickValueConfidence = QuickValueConfidence.MEDIUM,
    val condition: QuickValueCondition = QuickValueCondition.FAIR,
    val koreaSaleRange: KoreaSaleRange = KoreaSaleRange(),
    val japanSaleRange: JapanSaleRange = JapanSaleRange(),
    val jpyToKrwRate: Double = 9.1,
    val observations: List<Observation> = emptyList(),
    val valueReasons: List<String> = emptyList(),
    val nextPhotoInstruction: String? = null,
    val caveats: List<String> = emptyList(),
    val rawJson: String = ""
)
