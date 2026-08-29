package com.armsone.denimdex.feature.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armsone.denimdex.core.design.*
import com.armsone.denimdex.core.domain.CrossMarketRecommendation
import com.armsone.denimdex.core.domain.MarketValueCalculator
import com.armsone.denimdex.core.model.QuickValueConfidence
import com.armsone.denimdex.core.model.QuickValueResult
import com.armsone.denimdex.core.model.RarityLevel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun QuickValueResultCard(
    result: QuickValueResult,
    isSaved: Boolean,
    onSaveToArchive: () -> Unit,
    onNextPhotoInstructionClicked: (() -> Unit)? = null,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val koreaNet = remember(result) {
        MarketValueCalculator.calculateKoreaNetProceeds(result.koreaSaleRange)
    }
    val japanNet = remember(result) {
        MarketValueCalculator.calculateJapanNetProceeds(result.japanSaleRange)
    }
    val comparison = remember(result) {
        MarketValueCalculator.calculateCrossMarketComparison(
            koreaRange = result.koreaSaleRange,
            japanRange = result.japanSaleRange,
            jpyToKrwRate = result.jpyToKrwRate
        )
    }

    val nfKorea = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    val nfJapan = remember { NumberFormat.getNumberInstance(Locale.JAPAN) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.SCAN_RESULT_CARD)
            .denimCard(padding = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ==========================================
        // 1. 한눈에 보는 결론 (Quick Conclusion)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DenimEyebrow("한눈에 보는 결론")

                // Confidence Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .testTag(DenimTestTags.SCAN_RESULT_CONFIDENCE_BADGE)
                        .clip(RoundedCornerShape(12.dp))
                        .background(result.confidence.color.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = when (result.confidence) {
                            QuickValueConfidence.HIGH -> Icons.Default.CheckCircle
                            QuickValueConfidence.MEDIUM -> Icons.Default.Help
                            QuickValueConfidence.LOW -> Icons.Default.Warning
                            QuickValueConfidence.UNKNOWN -> Icons.Default.HelpOutline
                        },
                        contentDescription = null,
                        tint = result.confidence.color,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = result.confidence.displayName,
                        style = DenimTypography.captionBold.copy(
                            color = result.confidence.color,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Product Guess Header
            val title = "${result.productGuess.brand} ${result.productGuess.model}".trim()
            Text(
                text = if (title.isNotBlank()) title else "데님 제품",
                style = DenimTypography.title2.copy(color = DenimColors.charcoal),
                modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_TITLE)
            )
            if (result.productGuess.era.isNotBlank()) {
                Text(
                    text = result.productGuess.era,
                    style = DenimTypography.captionBold.copy(color = DenimColors.indigoBright),
                    modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_ERA)
                )
            }

            // Highlight Conclusion: Rarity & Fair Purchase Range
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DenimColors.fadedDenim.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "보수적 희귀도",
                        style = DenimTypography.caption.copy(fontSize = 11.sp, color = DenimColors.inkSoft)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = result.rarityLevel.displayName,
                            style = DenimTypography.captionBold.copy(
                                color = result.rarityLevel.color,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                Divider(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp),
                    color = DenimColors.hairline
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "적정 매입가 (한국)",
                        style = DenimTypography.caption.copy(fontSize = 11.sp, color = DenimColors.inkSoft)
                    )
                    Text(
                        text = "KRW ${nfKorea.format(result.koreaFairPurchaseRange.low)} ~ ${nfKorea.format(result.koreaFairPurchaseRange.high)}",
                        style = DenimTypography.captionBold.copy(
                            color = DenimColors.indigo,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            if (result.summary.isNotBlank()) {
                Text(
                    text = result.summary,
                    style = DenimTypography.subheadline.copy(color = DenimColors.inkSoft),
                    modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_SUMMARY)
                )
            }
        }

        Divider(color = DenimColors.hairline)

        // ==========================================
        // 2. 제품 정보 (Product Information)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DenimSectionTitle(title = "제품 정보")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DenimColors.fadedDenim.copy(alpha = 0.4f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttributeRow("브랜드", result.productGuess.brand.ifBlank { "확인되지 않음" })
                AttributeRow("모델", result.productGuess.model.ifBlank { "확인되지 않음" })
                AttributeRow("추정 연대", result.productGuess.era.ifBlank { "확인되지 않음" })
                AttributeRow("추정 생산연도", result.productGuess.estimatedProductionYear.ifBlank { "확인되지 않음" })
                AttributeRow("추정 제조공장", result.productGuess.estimatedFactory.ifBlank { "확인되지 않음" })
                if (result.productGuess.variant.isNotBlank()) {
                    AttributeRow("세부 디테일", result.productGuess.variant)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DenimTestTags.SCAN_RESULT_CONDITION),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "컨디션",
                        style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                    )
                    Text(
                        text = result.condition.displayName,
                        style = DenimTypography.captionBold.copy(color = DenimColors.charcoal)
                    )
                }
            }

            // Value Reasons
            if (result.valueReasons.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DenimTestTags.SCAN_RESULT_VALUE_REASONS),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "가치를 만든 디테일",
                        style = DenimTypography.captionBold.copy(color = DenimColors.charcoal)
                    )
                    result.valueReasons.forEach { reason ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = DenimColors.indigoBright,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reason,
                                style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                            )
                        }
                    }
                }
            }
        }

        Divider(color = DenimColors.hairline)

        // ==========================================
        // 3. 희귀도 (Rarity)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DenimSectionTitle(title = "희귀도", detail = "AI 추정")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DenimColors.fadedDenim.copy(alpha = 0.5f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "희귀도 등급",
                        style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(result.rarityLevel.color.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = result.rarityLevel.displayName,
                            style = DenimTypography.captionBold.copy(
                                color = result.rarityLevel.color,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                if (result.raritySummary.isNotBlank()) {
                    Text(
                        text = result.raritySummary,
                        style = DenimTypography.caption.copy(color = DenimColors.charcoal)
                    )
                }

                if (result.rarityReasons.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        result.rarityReasons.forEach { reason ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "·",
                                    style = DenimTypography.captionBold.copy(color = DenimColors.brass),
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = reason,
                                    style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "※ AI 기반 보수적 추정치이며 검증된 희소성이 아닙니다.",
                    style = DenimTypography.caption2.copy(color = DenimColors.inkSoft.copy(alpha = 0.7f), letterSpacing = 0.sp)
                )
            }
        }

        Divider(color = DenimColors.hairline)

        // ==========================================
        // 4. 적정 금액 (Fair Amount & Dual Market)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DenimSectionTitle(title = "적정 금액")

            // Dual Market Cards Row (Korea vs Japan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Korea Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(DenimTestTags.SCAN_RESULT_KOREA_CARD)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DenimColors.fadedDenim.copy(alpha = 0.6f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "한국",
                        style = DenimTypography.captionBold.copy(color = DenimColors.indigo)
                    )
                    Text(
                        text = "적정 매입가",
                        style = DenimTypography.caption.copy(fontSize = 10.sp, color = DenimColors.inkSoft)
                    )
                    Text(
                        text = "KRW ${nfKorea.format(result.koreaFairPurchaseRange.low)} ~ ${nfKorea.format(result.koreaFairPurchaseRange.high)}",
                        style = DenimTypography.captionBold.copy(color = DenimColors.indigo, fontSize = 12.sp)
                    )
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = DenimColors.hairline
                    )
                    Text(
                        text = "예상 판매가",
                        style = DenimTypography.caption.copy(fontSize = 10.sp, color = DenimColors.inkSoft)
                    )
                    Text(
                        text = "KRW ${nfKorea.format(result.koreaSaleRange.low)} ~ ${nfKorea.format(result.koreaSaleRange.high)}",
                        style = DenimTypography.captionBold.copy(color = DenimColors.charcoal, fontSize = 12.sp),
                        modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_KOREA_SALE_RANGE)
                    )
                    Text(
                        text = "순수익 추정",
                        style = DenimTypography.caption.copy(fontSize = 10.sp, color = DenimColors.inkSoft)
                    )
                    Text(
                        text = "KRW ${nfKorea.format(koreaNet.low)} ~ ${nfKorea.format(koreaNet.high)}",
                        style = DenimTypography.captionBold.copy(color = DenimColors.successGreen, fontSize = 12.sp),
                        modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_KOREA_NET_PROCEEDS)
                    )
                }

                // Japan Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(DenimTestTags.SCAN_RESULT_JAPAN_CARD)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DenimColors.fadedDenim.copy(alpha = 0.6f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "일본",
                        style = DenimTypography.captionBold.copy(color = DenimColors.coolBlue)
                    )
                    Text(
                        text = "적정 매입가",
                        style = DenimTypography.caption.copy(fontSize = 10.sp, color = DenimColors.inkSoft)
                    )
                    Text(
                        text = "JPY ${nfJapan.format(result.japanFairPurchaseRange.low)} ~ ${nfJapan.format(result.japanFairPurchaseRange.high)}",
                        style = DenimTypography.captionBold.copy(color = DenimColors.coolBlue, fontSize = 12.sp)
                    )
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = DenimColors.hairline
                    )
                    Text(
                        text = "예상 판매가",
                        style = DenimTypography.caption.copy(fontSize = 10.sp, color = DenimColors.inkSoft)
                    )
                    Text(
                        text = "JPY ${nfJapan.format(result.japanSaleRange.low)} ~ ${nfJapan.format(result.japanSaleRange.high)}",
                        style = DenimTypography.captionBold.copy(color = DenimColors.charcoal, fontSize = 12.sp),
                        modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_JAPAN_SALE_RANGE)
                    )
                    Text(
                        text = "순수익 추정",
                        style = DenimTypography.caption.copy(fontSize = 10.sp, color = DenimColors.inkSoft)
                    )
                    Text(
                        text = "JPY ${nfJapan.format(japanNet.low)} ~ ${nfJapan.format(japanNet.high)}",
                        style = DenimTypography.captionBold.copy(color = DenimColors.successGreen, fontSize = 12.sp),
                        modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_JAPAN_NET_PROCEEDS)
                    )
                }
            }

            // Cross-Market Arbitrage Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DenimTestTags.SCAN_RESULT_ARBITRAGE_SECTION)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DenimColors.fadedDenim.copy(alpha = 0.72f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DenimSectionTitle(title = "시장별 판매 기회")

                // J2K
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "일본 구매 → 한국 판매",
                        style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                    )
                    val j2kLow = comparison.japanToKoreaMargin.low
                    val j2kHigh = comparison.japanToKoreaMargin.high
                    val isJ2kProfit = j2kLow >= 0L
                    Text(
                        text = "KRW ${nfKorea.format(j2kLow)} ~ ${nfKorea.format(j2kHigh)}",
                        style = DenimTypography.captionBold.copy(
                            color = if (isJ2kProfit) DenimColors.successGreen else DenimColors.signalRed
                        ),
                        modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_J2K_MARGIN)
                    )
                }

                // K2J
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "한국 구매 → 일본 판매",
                        style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                    )
                    val k2jLow = comparison.koreaToJapanMargin.low
                    val k2jHigh = comparison.koreaToJapanMargin.high
                    val isK2jProfit = k2jLow >= 0L
                    Text(
                        text = "KRW ${nfKorea.format(k2jLow)} ~ ${nfKorea.format(k2jHigh)}",
                        style = DenimTypography.captionBold.copy(
                            color = if (isK2jProfit) DenimColors.successGreen else DenimColors.signalRed
                        ),
                        modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_K2J_MARGIN)
                    )
                }

                // Recommendation Text
                Text(
                    text = comparison.recommendation.message,
                    style = DenimTypography.captionBold.copy(
                        color = when (comparison.recommendation) {
                            CrossMarketRecommendation.JAPAN_TO_KOREA,
                            CrossMarketRecommendation.KOREA_TO_JAPAN -> DenimColors.successGreen
                            CrossMarketRecommendation.NO_CLEAR_ADVANTAGE -> DenimColors.inkSoft
                        }
                    ),
                    modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_ARBITRAGE_RECOMMENDATION)
                )

                // Arbitrage Footnote
                Text(
                    text = "국제 배송비 30,000원과 판매 수수료 10%를 반영한 추정입니다. 관세·세금·환전 수수료·반품·환율 변동은 포함되지 않습니다.",
                    style = DenimTypography.caption2.copy(
                        color = DenimColors.inkSoft.copy(alpha = 0.8f),
                        letterSpacing = 0.sp
                    )
                )
            }

            // Disclaimer Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DenimTestTags.SCAN_RESULT_DISCLAIMER)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DenimColors.signalRed.copy(alpha = 0.045f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = DenimColors.signalRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "현재 가격은 실시간 거래 자료가 아닌 AI 기반 추정치입니다.",
                        style = DenimTypography.captionBold.copy(color = DenimColors.signalRed, fontSize = 12.sp)
                    )
                }
                Text(
                    text = "한국: 판매 수수료 10%와 국내 배송비 5,000원 반영",
                    style = DenimTypography.caption.copy(fontSize = 11.sp, color = DenimColors.inkSoft)
                )
                Text(
                    text = "일본: 판매 수수료 10%와 국내 배송비 1,000엔 반영",
                    style = DenimTypography.caption.copy(fontSize = 11.sp, color = DenimColors.inkSoft)
                )
                Text(
                    text = "적용 환율: 1엔 ≈ ${String.format(Locale.US, "%.2f", result.jpyToKrwRate)}원",
                    style = DenimTypography.caption.copy(fontSize = 11.sp, color = DenimColors.inkSoft)
                )
            }

            // Caveats
            if (result.caveats.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DenimTestTags.SCAN_RESULT_CAVEATS),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    result.caveats.forEach { caveat ->
                        Text(
                            text = "· $caveat",
                            style = DenimTypography.caption2.copy(color = DenimColors.inkSoft.copy(alpha = 0.8f), letterSpacing = 0.sp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 5. Action Buttons
        // ==========================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Save to Archive Button
            DenimPrimaryButton(
                text = if (isSaved) "보관 완료" else "내 아카이브에 보관",
                onClick = onSaveToArchive,
                enabled = !isSaved,
                modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_SAVE_BUTTON),
                leadingIcon = {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Check else Icons.Default.SaveAlt,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            )

            // Next photo instruction button if available
            if (!result.nextPhotoInstruction.isNullOrBlank() && onNextPhotoInstructionClicked != null) {
                DenimSecondaryButton(
                    text = result.nextPhotoInstruction,
                    onClick = onNextPhotoInstructionClicked,
                    modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_NEXT_INSTRUCTION_BUTTON),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = DenimColors.indigoBright
                        )
                    }
                )
            }

            // Restart Button
            DenimSecondaryButton(
                text = "새로 감정하기",
                onClick = onRestart,
                modifier = Modifier.testTag(DenimTestTags.SCAN_RESULT_RESTART_BUTTON)
            )

            // Footnote
            Text(
                text = "거래 근거를 더하는 정밀 조사는 준비 중입니다.",
                style = DenimTypography.caption2.copy(color = DenimColors.inkSoft.copy(alpha = 0.7f), letterSpacing = 0.sp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun AttributeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = DenimTypography.caption.copy(color = DenimColors.inkSoft))
        Text(text = value, style = DenimTypography.captionBold.copy(color = DenimColors.charcoal))
    }
}
