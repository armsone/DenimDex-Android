package com.armsone.denimdex.feature.guide

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armsone.denimdex.core.design.*
import com.armsone.denimdex.core.model.EvidencePhotoRole
import com.armsone.denimdex.feature.scan.GuidedCaptureStep
import com.armsone.denimdex.feature.scan.GuidedCapturePresets
import com.armsone.denimdex.feature.scan.GuidedReferencePreviewOverlay

@Composable
fun LearnScreen(modifier: Modifier = Modifier) {
    var referencePreviewStep by remember { mutableStateOf<GuidedCaptureStep?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(DenimTestTags.GUIDE_SCREEN)
            .background(DenimColors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "감정 가이드",
            style = DenimTypography.largeTitle,
            modifier = Modifier.testTag(DenimTestTags.GUIDE_TITLE)
        )

        // Top banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DenimTestTags.GUIDE_BANNER)
                .clip(RoundedCornerShape(20.dp))
                .background(brush = DenimColors.indigoGradient)
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DenimEyebrow("Field Guide", color = DenimColors.washedDenim)
                Text(
                    text = "디테일을 기록하는\n가장 좋은 방법",
                    style = DenimTypography.title1.copy(fontSize = 26.sp, color = Color.White, lineHeight = 32.sp)
                )
                Text(
                    text = "더 정교한 감정을 위한 데님 촬영 가이드",
                    style = DenimTypography.caption.copy(color = Color.White.copy(alpha = 0.68f))
                )
            }
        }

        GuideSection(
            icon = Icons.Default.CameraAlt,
            title = "사진은 몇 장이 좋을까요?",
            modifier = Modifier.testTag(DenimTestTags.GUIDE_SECTION_COUNT)
        ) {
            Text(
                text = "1장부터 시작할 수 있으며, 최대 30장까지 모아 담을 수 있어요. 담은 사진 중 가장 선명한 사진을 자동으로 선별해 분석에 사용합니다.",
                style = DenimTypography.body.copy(color = DenimColors.inkSoft)
            )
        }

        GuideSection(
            icon = Icons.Default.CheckCircle,
            title = "가치를 잘 보여주는 촬영법",
            modifier = Modifier.testTag(DenimTestTags.GUIDE_SECTION_SHOOTING)
        ) {
            listOf(
                "전체 실루엣" to "밝은 곳에서 정면 전신 샷을 촬영해주세요.",
                "레드탭과 패치" to "초점을 맞춘 근접 샷으로 촬영해주세요.",
                "버튼과 리벳" to "그림자 없이 각인이 잘 보이도록 촬영해주세요.",
                "케어 라벨" to "주름을 펴고 글자가 또렷하게 보이도록 촬영해주세요."
            ).forEach { (label, desc) ->
                BulletLine(label = label, description = desc)
            }
        }

        GuideSection(
            icon = Icons.Default.Checkroom,
            title = "팬츠 9컷 촬영 순서",
            modifier = Modifier.testTag(DenimTestTags.GUIDE_SECTION_PANTS_STEPS)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GuidedCapturePresets.pantsSteps.forEachIndexed { index, step ->
                    GuidedStepLine(
                        order = index + 1,
                        step = step,
                        onShowReference = { referencePreviewStep = step }
                    )
                }
            }
        }

        GuideSection(
            icon = Icons.Default.Checkroom,
            title = "재킷 9컷 촬영 순서",
            modifier = Modifier.testTag(DenimTestTags.GUIDE_SECTION_JACKET_STEPS)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GuidedCapturePresets.jacketSteps.forEachIndexed { index, step ->
                    GuidedStepLine(
                        order = index + 1,
                        step = step,
                        onShowReference = { referencePreviewStep = step }
                    )
                }
            }
        }

        GuideSection(
            icon = Icons.Default.Sell,
            title = "확인하면 좋은 디테일",
            modifier = Modifier.testTag(DenimTestTags.GUIDE_SECTION_DETAILS)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EvidencePhotoRole.entries.forEach { role ->
                    Text(
                        text = "· ${role.title} — ${role.description}",
                        style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                    )
                }
            }
        }

        GuideSection(
            icon = Icons.Default.Speed,
            title = "컨디션 기준",
            modifier = Modifier.testTag(DenimTestTags.GUIDE_SECTION_CONDITION)
        ) {
            listOf(
                "최상" to "사용감이 거의 없는 상태",
                "양호" to "자연스러운 페이딩만 있는 상태",
                "보통" to "일반적인 사용 흔적이 있는 상태",
                "사용감 많음" to "손상이나 수선 흔적이 뚜렷한 상태"
            ).forEach { (label, desc) ->
                BulletLine(label = label, description = desc)
            }
        }

        GuideSection(
            icon = Icons.Default.Speed,
            title = "판단 신뢰도",
            modifier = Modifier.testTag(DenimTestTags.GUIDE_SECTION_CONFIDENCE)
        ) {
            Text(
                text = "일치하는 특징이 많을수록 신뢰도가 높게 산정됩니다. 근거가 부족한 경우 무리하게 단정하지 않고 신뢰도를 낮춰 보여드립니다.",
                style = DenimTypography.body.copy(color = DenimColors.inkSoft)
            )
        }

        GuideSection(
            icon = Icons.Default.Paid,
            title = "두 가지 가치 기준",
            modifier = Modifier.testTag(DenimTestTags.GUIDE_SECTION_VALUE_BASIS)
        ) {
            Text(
                text = "빠른 AI 추정: 사진 기반으로 즉시 산출하는 참고용 가치입니다.\n근거 조사(정밀 조사): 거래 근거를 더하는 기능은 현재 준비 중입니다.",
                style = DenimTypography.body.copy(color = DenimColors.inkSoft)
            )
        }

        GuideSection(
            icon = Icons.Default.Info,
            title = "이용 전 확인해주세요",
            modifier = Modifier.testTag(DenimTestTags.GUIDE_SECTION_DISCLAIMER)
        ) {
            Text(
                text = "DenimDex의 감정 결과는 정품 인증서나 공식 감정서가 아니며, AI 기반의 빠른 참고용 추정치입니다. 실제 거래 전에는 반드시 전문가의 확인을 거치시기 바랍니다.",
                style = DenimTypography.body.copy(color = DenimColors.inkSoft)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Reference Image Enlargement Overlay (tap anywhere to dismiss)
    referencePreviewStep?.let { step ->
        GuidedReferencePreviewOverlay(
            previewRes = step.previewRes,
            contentDescription = "${step.title} 참고 이미지 확대",
            onDismiss = { referencePreviewStep = null }
        )
    }
    }
}

@Composable
private fun GuidedStepLine(
    order: Int,
    step: GuidedCaptureStep,
    onShowReference: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shape = RoundedCornerShape(10.dp)
        Image(
            painter = painterResource(step.thumbnailRes),
            contentDescription = "${step.title} 참고 이미지",
            modifier = Modifier
                .size(44.dp)
                .clip(shape)
                .border(1.dp, DenimColors.hairline, shape)
                .clickable { onShowReference() },
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$order. ${step.title}",
                style = DenimTypography.captionBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = step.instruction,
                style = DenimTypography.caption.copy(color = DenimColors.inkSoft),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GuideSection(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .denimCard(padding = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DenimColors.indigoBright,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = DenimTypography.headline.copy(color = DenimColors.charcoal))
        }
        content()
    }
}

@Composable
private fun BulletLine(label: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = "· $label: ",
            style = DenimTypography.captionBold.copy(color = DenimColors.charcoal)
        )
        Text(
            text = description,
            style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
        )
    }
}
