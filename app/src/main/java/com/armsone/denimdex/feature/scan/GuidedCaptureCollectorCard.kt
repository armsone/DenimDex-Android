package com.armsone.denimdex.feature.scan

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.armsone.denimdex.core.design.*

/**
 * Three-way capture mode selector: 팬츠 / 재킷 / 자유 촬영.
 */
@Composable
fun CaptureModeSelector(
    selected: ScanCaptureMode,
    onSelect: (ScanCaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.SCAN_MODE_SELECTOR)
            .clip(DenimShapes.pill)
            .background(DenimColors.fadedDenim)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScanCaptureMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .testTag(DenimTestTags.scanModeButton(mode.name.lowercase()))
                    .clip(DenimShapes.pill)
                    .background(if (isSelected) DenimColors.indigo else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.displayName,
                    style = DenimTypography.captionBold.copy(
                        color = if (isSelected) Color.White else DenimColors.inkSoft
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Nine-slot guided collector for pants/jacket: heading, captured count, progress,
 * per-row reference thumbnail (tap to enlarge), camera/library actions,
 * clear-current-mode and full guided-camera start/continue actions.
 */
@Composable
fun GuidedCaptureCollectorCard(
    mode: ScanCaptureMode,
    slots: List<GuidedSlotState>,
    onRowCamera: (Int) -> Unit,
    onRowLibrary: (Int) -> Unit,
    onShowReference: (GuidedCaptureStep) -> Unit,
    onStartGuidedCamera: () -> Unit,
    onRequestClear: () -> Unit,
    hasCamera: Boolean = true,
    modifier: Modifier = Modifier
) {
    val steps = GuidedCapturePresets.steps(mode)
    val capturedCount = slots.count { it.isCaptured }
    val anyProgress = slots.any { it.isResolved }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.SCAN_GUIDED_COLLECTOR)
            .denimCard(padding = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = GuidedCapturePresets.heading(mode), style = DenimTypography.title3)
                Text(text = "순서대로 아홉 부위를 담아주세요", style = DenimTypography.caption)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .testTag(DenimTestTags.SCAN_GUIDED_COUNT_BADGE)
                        .clip(DenimShapes.pill)
                        .background(DenimColors.fadedDenim)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$capturedCount / ${steps.size}",
                        style = DenimTypography.captionBold.copy(color = DenimColors.indigo)
                    )
                }

                if (anyProgress) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onRequestClear,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag(DenimTestTags.SCAN_GUIDED_CLEAR_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "현재 모드 비우기",
                            tint = DenimColors.signalRed
                        )
                    }
                }
            }
        }

        LinearProgressIndicator(
            progress = { if (steps.isEmpty()) 0f else capturedCount.toFloat() / steps.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .testTag(DenimTestTags.SCAN_GUIDED_PROGRESS),
            color = DenimColors.indigo,
            trackColor = DenimColors.fadedDenim
        )

        // Slot rows
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            steps.forEachIndexed { index, step ->
                GuidedSlotRow(
                    index = index,
                    step = step,
                    slot = slots.getOrNull(index) ?: GuidedSlotState(),
                    onCamera = { onRowCamera(index) },
                    onLibrary = { onRowLibrary(index) },
                    onShowReference = { onShowReference(step) },
                    hasCamera = hasCamera
                )
            }
        }

        DenimPrimaryButton(
            text = if (anyProgress) "가이드 촬영 이어서 하기" else "가이드 촬영 시작",
            onClick = onStartGuidedCamera,
            enabled = hasCamera,
            modifier = Modifier.testTag(DenimTestTags.SCAN_GUIDED_START_BUTTON),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        )
    }
}

@Composable
private fun GuidedSlotRow(
    index: Int,
    step: GuidedCaptureStep,
    slot: GuidedSlotState,
    onCamera: () -> Unit,
    onLibrary: () -> Unit,
    onShowReference: () -> Unit,
    hasCamera: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.scanGuidedRow(index)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reference thumbnail, replaced by the captured photo once taken
        val thumbnailShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(58.dp)
                .testTag(DenimTestTags.scanGuidedRowThumbnail(index))
                .clip(thumbnailShape)
                .background(DenimColors.offWhite)
                .border(1.dp, DenimColors.hairline, thumbnailShape)
                .clickable { onShowReference() }
        ) {
            val photo = slot.photo
            if (photo != null) {
                val bitmap = remember(photo) {
                    try {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeByteArray(photo, 0, photo.size, opts)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = step.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Image(
                    painter = painterResource(step.thumbnailRes),
                    contentDescription = "${step.title} 참고 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}. ${step.title}",
                    style = DenimTypography.captionBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (slot.isCaptured) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "촬영 완료",
                        tint = DenimColors.successGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (slot.isSkipped && !slot.isCaptured) {
                Text(
                    text = "건너뜀",
                    style = DenimTypography.caption.copy(color = DenimColors.warningAmber)
                )
            } else {
                Text(
                    text = step.instruction,
                    style = DenimTypography.caption.copy(color = DenimColors.inkSoft),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        if (hasCamera) {
            IconButton(
                onClick = onCamera,
                modifier = Modifier
                    .size(36.dp)
                    .testTag(DenimTestTags.scanGuidedRowCameraButton(index))
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "${step.title} 촬영",
                    tint = DenimColors.indigoBright,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        IconButton(
            onClick = onLibrary,
            modifier = Modifier
                .size(36.dp)
                .testTag(DenimTestTags.scanGuidedRowLibraryButton(index))
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "${step.title} 사진 선택",
                tint = DenimColors.indigoBright,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Dim full-screen overlay showing the high-resolution reference preview.
 * Tapping anywhere dismisses it.
 */
@Composable
fun GuidedReferencePreviewOverlay(
    @DrawableRes previewRes: Int,
    contentDescription: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(DenimTestTags.SCAN_REFERENCE_PREVIEW_OVERLAY)
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(previewRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Fit
        )
    }
}
