package com.armsone.denimdex.feature.archive

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armsone.denimdex.core.design.*
import com.armsone.denimdex.core.model.CollectionItem
import com.armsone.denimdex.core.model.SyncEligibilityState
import com.armsone.denimdex.core.model.VerificationState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionItemDetailScreen(
    item: CollectionItem,
    onBack: () -> Unit,
    onUpdate: (CollectionItem) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var userTitle by remember(item.id) { mutableStateOf(item.userTitle) }
    var userNotes by remember(item.id) { mutableStateOf(item.userNotes) }
    var verificationState by remember(item.id) { mutableStateOf(item.verificationState) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun persist(newTitle: String = userTitle, newNotes: String = userNotes, newState: VerificationState = verificationState) {
        val syncState = if (newState == VerificationState.USER_CONFIRMED &&
            item.syncEligibilityState == SyncEligibilityState.NOT_ELIGIBLE
        ) {
            SyncEligibilityState.PENDING_CONSENT
        } else {
            item.syncEligibilityState
        }
        onUpdate(
            item.copy(
                userTitle = newTitle,
                userNotes = newNotes,
                verificationState = newState,
                syncEligibilityState = syncState
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(DenimTestTags.ARCHIVE_DETAIL_SCREEN)
            .background(DenimColors.canvas)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag(DenimTestTags.ARCHIVE_DETAIL_BACK_BUTTON)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로")
            }
            Text(
                text = "아카이브 상세",
                style = DenimTypography.headline.copy(color = DenimColors.charcoal),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.testTag(DenimTestTags.ARCHIVE_DETAIL_DELETE_ICON_BUTTON)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = DenimColors.signalRed)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Photo carousel 244x270
            if (item.photoUris.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DenimTestTags.ARCHIVE_DETAIL_PHOTO_CAROUSEL)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item.photoUris.forEach { uriStr ->
                        val bitmap = remember(uriStr) {
                            try {
                                val path = Uri.parse(uriStr).path
                                if (path != null && File(path).exists()) {
                                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                                    BitmapFactory.decodeFile(path, opts)
                                } else null
                            } catch (_: Exception) {
                                null
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(244.dp)
                                .height(270.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DenimColors.offWhite)
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // User title
            OutlinedTextField(
                value = userTitle,
                onValueChange = {
                    userTitle = it
                    persist(newTitle = it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DenimTestTags.ARCHIVE_DETAIL_TITLE_FIELD),
                placeholder = { Text("이름을 지어주세요") },
                textStyle = DenimTypography.title2.copy(color = DenimColors.charcoal),
                singleLine = true
            )

            // Price & value basis card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DenimTestTags.ARCHIVE_DETAIL_VALUE_CARD)
                    .denimCard(padding = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.hasFairPurchaseRange && item.formattedFairPurchaseRange != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "적정 매입가",
                            style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                        )
                        Text(
                            text = item.formattedFairPurchaseRange ?: "",
                            style = DenimTypography.title3.copy(color = DenimColors.indigo)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "예상 판매가",
                            style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                        )
                        Text(
                            text = item.formattedValueRange,
                            style = DenimTypography.title3.copy(color = DenimColors.charcoal)
                        )
                    }
                } else {
                    Text(
                        text = item.formattedValueRange,
                        style = DenimTypography.title2.copy(color = DenimColors.charcoal)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = DenimColors.brass,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.valueBasis.badgeText,
                        style = DenimTypography.captionBold.copy(color = DenimColors.brass)
                    )
                }
            }

            // Attribute grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DenimTestTags.ARCHIVE_DETAIL_ATTRIBUTE_GRID)
                    .denimCard(padding = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AttributeRow("브랜드", item.brandGuess.ifBlank { "확인되지 않음" })
                AttributeRow("모델", item.modelGuess.ifBlank { "확인되지 않음" })
                AttributeRow("추정 연대", item.eraGuess.ifBlank { "확인되지 않음" })
                AttributeRow("추정 생산연도", item.estimatedProductionYear.ifBlank { "확인되지 않음" })
                AttributeRow("추정 제조공장", item.estimatedFactory.ifBlank { "확인되지 않음" })
                if (item.hasVariantInfo) {
                    AttributeRow("세부 디테일", item.variantGuess)
                }
                AttributeRow("컨디션", item.condition.displayName)
                AttributeRow("판단 신뢰도", item.confidence.displayName)
                AttributeRow("기록일", formatDate(item.createdAt))
            }

            // Rarity section (V3-only, hidden for legacy items)
            if (item.hasRarityInfo) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .denimCard(padding = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "희귀도 (AI 추정)",
                            style = DenimTypography.captionBold.copy(color = DenimColors.charcoal)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(item.rarityLevel.color.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.rarityLevel.displayName,
                                style = DenimTypography.captionBold.copy(
                                    color = item.rarityLevel.color,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                    if (item.raritySummary.isNotBlank()) {
                        Text(
                            text = item.raritySummary,
                            style = DenimTypography.caption.copy(color = DenimColors.charcoal)
                        )
                    }
                    if (item.rarityReasons.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            item.rarityReasons.forEach { reason ->
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
                }
            }

            // Summary
            if (item.summary.isNotBlank()) {
                Text(
                    text = item.summary,
                    style = DenimTypography.body.copy(color = DenimColors.inkSoft),
                    modifier = Modifier.testTag(DenimTestTags.ARCHIVE_DETAIL_SUMMARY)
                )
            }

            // Verification state segmented control
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "확인 상태", style = DenimTypography.captionBold.copy(color = DenimColors.charcoal))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DenimTestTags.ARCHIVE_DETAIL_VERIFICATION_SELECTOR)
                ) {
                    val options = listOf(VerificationState.AI_ESTIMATE, VerificationState.USER_CONFIRMED)
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = verificationState == option,
                            onClick = {
                                verificationState = option
                                persist(newState = option)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                        ) {
                            Text(option.displayName)
                        }
                    }
                }
            }

            // Notes editor
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "메모", style = DenimTypography.captionBold.copy(color = DenimColors.charcoal))
                OutlinedTextField(
                    value = userNotes,
                    onValueChange = {
                        userNotes = it
                        persist(newNotes = it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp)
                        .testTag(DenimTestTags.ARCHIVE_DETAIL_NOTES_FIELD),
                    textStyle = DenimTypography.body.copy(color = DenimColors.charcoal)
                )
            }

            // Delete button
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DenimTestTags.ARCHIVE_DETAIL_DELETE_BUTTON),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DenimColors.signalRed)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("아카이브에서 삭제", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            modifier = Modifier.testTag(DenimTestTags.DIALOG_DELETE_ITEM),
            title = { Text(text = "아카이브에서 삭제할까요?", style = DenimTypography.title3.copy(color = DenimColors.charcoal)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(item.id)
                        onBack()
                    },
                    modifier = Modifier.testTag(DenimTestTags.DIALOG_DELETE_ITEM_CONFIRM_BUTTON)
                ) {
                    Text("아카이브에서 삭제", style = DenimTypography.headline.copy(color = DenimColors.signalRed))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.testTag(DenimTestTags.DIALOG_DELETE_ITEM_CANCEL_BUTTON)
                ) {
                    Text("취소", style = DenimTypography.body.copy(color = DenimColors.inkSoft))
                }
            }
        )
    }
}

@Composable
private fun AttributeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = DenimTypography.caption.copy(color = DenimColors.inkSoft))
        Text(text = value, style = DenimTypography.captionBold.copy(color = DenimColors.charcoal))
    }
}

private fun formatDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
    return sdf.format(Date(epochMs))
}
