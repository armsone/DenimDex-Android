package com.armsone.denimdex.feature.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.armsone.denimdex.core.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncInviteSheet(
    onDismiss: () -> Unit,
    onStartSync: () -> Unit,
    onViewDisclosure: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DenimTestTags.SHEET_SYNC_INVITE)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DenimEyebrow("Collective Archive")
            Text(
                text = "아카이브를 확장할까요?",
                style = DenimTypography.title2.copy(color = DenimColors.charcoal)
            )
            Text(
                text = "다른 컬렉터의 기록으로 내 아카이브를 넓히고,\n내 기록도 익명으로 함께 나눕니다.",
                style = DenimTypography.body.copy(color = DenimColors.inkSoft)
            )

            Spacer(modifier = Modifier.height(4.dp))

            DenimPrimaryButton(
                text = "지금 확장하기",
                onClick = onStartSync,
                modifier = Modifier.testTag(DenimTestTags.SHEET_SYNC_INVITE_START_BUTTON)
            )

            DenimSecondaryButton(
                text = "나중에",
                onClick = onDismiss,
                modifier = Modifier.testTag(DenimTestTags.SHEET_SYNC_INVITE_LATER_BUTTON)
            )

            TextButton(
                onClick = onViewDisclosure,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(DenimTestTags.SHEET_SYNC_INVITE_DISCLOSURE_BUTTON)
            ) {
                Text(
                    text = "공유되는 정보 확인",
                    style = DenimTypography.captionBold.copy(color = DenimColors.indigoBright)
                )
            }
        }
    }
}

private data class DisclosureItem(val label: String, val shared: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDisclosureSheet(
    onDismiss: () -> Unit
) {
    val sharedItems = remember {
        listOf(
            "브랜드", "모델", "추정 연대", "확인된 특징", "컨디션",
            "가격 범위", "통화", "국가", "기록일과 익명 식별 정보"
        )
    }
    val notSharedItems = remember {
        listOf("원본 사진", "이름", "이메일", "위치", "ChatGPT 로그인 정보와 대화 원문")
    }
    val checkedStates = remember { mutableStateMapOf<String, Boolean>().apply {
        sharedItems.forEach { put(it, true) }
    } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DenimTestTags.SHEET_SYNC_DISCLOSURE)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "공유 정보",
                style = DenimTypography.title2.copy(color = DenimColors.charcoal)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "함께 나누는 정보",
                    style = DenimTypography.captionBold.copy(color = DenimColors.successGreen)
                )
                sharedItems.forEach { label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DenimColors.fadedDenim.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkedStates[label] ?: true,
                            onCheckedChange = { checkedStates[label] = it }
                        )
                        Text(text = label, style = DenimTypography.body.copy(color = DenimColors.charcoal))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "공유하지 않는 정보",
                    style = DenimTypography.captionBold.copy(color = DenimColors.signalRed)
                )
                notSharedItems.forEach { label ->
                    Text(
                        text = "· $label",
                        style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                    )
                }
            }

            DenimSecondaryButton(
                text = "닫기",
                onClick = onDismiss,
                modifier = Modifier.testTag(DenimTestTags.SHEET_SYNC_DISCLOSURE_CLOSE_BUTTON)
            )
        }
    }
}
