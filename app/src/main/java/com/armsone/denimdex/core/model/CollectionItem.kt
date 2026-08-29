package com.armsone.denimdex.core.model

import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

data class CollectionItem(
    val id: String = UUID.randomUUID().toString(),
    val userTitle: String = "",
    val brandGuess: String = "",
    val modelGuess: String = "",
    val eraGuess: String = "",
    val variantGuess: String = "",
    val estimatedProductionYear: String = "",
    val estimatedFactory: String = "",
    val summary: String = "",
    val confidence: QuickValueConfidence = QuickValueConfidence.MEDIUM,
    val condition: QuickValueCondition = QuickValueCondition.FAIR,
    val rarityLevel: RarityLevel = RarityLevel.UNKNOWN,
    val raritySummary: String = "",
    val rarityReasons: List<String> = emptyList(),
    val koreaFairPurchaseLow: Long = 0L,
    val koreaFairPurchaseHigh: Long = 0L,
    val japanFairPurchaseLow: Long = 0L,
    val japanFairPurchaseHigh: Long = 0L,
    val koreaSaleLow: Long = 0L,
    val koreaSaleHigh: Long = 0L,
    val japanSaleLow: Long = 0L,
    val japanSaleHigh: Long = 0L,
    val jpyToKrwRate: Double = 9.1,
    val verificationState: VerificationState = VerificationState.AI_ESTIMATE,
    val syncEligibilityState: SyncEligibilityState = SyncEligibilityState.NOT_ELIGIBLE,
    val userNotes: String = "",
    val photoUris: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val rawAiResponseJson: String = ""
) {
    val displayTitle: String
        get() {
            if (userTitle.isNotBlank()) return userTitle.trim()
            val combined = "$brandGuess $modelGuess".trim()
            return if (combined.isNotEmpty()) combined else "이름 없는 데님"
        }

    val formattedValueRange: String
        get() {
            val nf = NumberFormat.getNumberInstance(Locale.KOREA)
            return "KRW ${nf.format(koreaSaleLow)} ~ ${nf.format(koreaSaleHigh)}"
        }

    val formattedFairPurchaseRange: String?
        get() {
            if (koreaFairPurchaseLow == 0L && koreaFairPurchaseHigh == 0L) return null
            val nf = NumberFormat.getNumberInstance(Locale.KOREA)
            return "KRW ${nf.format(koreaFairPurchaseLow)} ~ ${nf.format(koreaFairPurchaseHigh)}"
        }

    val formattedJapanValueRange: String
        get() {
            val nf = NumberFormat.getNumberInstance(Locale.JAPAN)
            return "JPY ${nf.format(japanSaleLow)} ~ ${nf.format(japanSaleHigh)}"
        }

    val hasFairPurchaseRange: Boolean
        get() = (koreaFairPurchaseLow > 0L || koreaFairPurchaseHigh > 0L) || (japanFairPurchaseLow > 0L || japanFairPurchaseHigh > 0L)

    val hasRarityInfo: Boolean
        get() = rarityLevel != RarityLevel.UNKNOWN || raritySummary.isNotBlank() || rarityReasons.isNotEmpty()

    val hasVariantInfo: Boolean
        get() = variantGuess.isNotBlank()

    val valueBasis: QuickValueBasis
        get() = QuickValueBasis.AI_QUICK_VALUE
}
