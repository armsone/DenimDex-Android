package com.armsone.denimdex.feature.scan

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.armsone.denimdex.core.design.*
import com.armsone.denimdex.core.domain.CountdownFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    hasCamera: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsState()
    val runState by viewModel.runner.state.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val latestResult by viewModel.latestResult.collectAsState()

    val showLoginSheet by viewModel.showLoginSheet.collectAsState()
    val showConsentDialog by viewModel.showConsentDialog.collectAsState()
    val showClearAllConfirm by viewModel.showClearAllConfirm.collectAsState()
    val showCamera by viewModel.showCamera.collectAsState()
    val showPhotoSaveAlert by viewModel.showPhotoSaveAlert.collectAsState()

    val isVisibleBrowserPresented by viewModel.runner.currentSession?.isVisibleBrowserPresented?.collectAsState()
        ?: remember { mutableStateOf(false) }

    // Hidden FrameLayout for WebView
    var hiddenContainerView: FrameLayout? by remember { mutableStateOf(null) }

    // Back key handling (8.2): result shown -> reset to collecting; visible browser -> cancel+dismiss
    androidx.activity.compose.BackHandler(
        enabled = runState is QuickValueRunState.Success ||
            (isVisibleBrowserPresented && runState is QuickValueRunState.Running)
    ) {
        if (isVisibleBrowserPresented && runState is QuickValueRunState.Running) {
            viewModel.runner.cancel()
        } else {
            viewModel.resetValuation()
        }
    }

    // System Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = (30 - photos.size).coerceIn(1, 30)
        )
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addPhotosFromUris(uris)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Hidden WebView container attached behind host
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(375, 667)
                    alpha = 0.001f
                    isClickable = false
                    isFocusable = false
                    hiddenContainerView = this
                    post { viewModel.refreshLoginStatus(this) }
                }
            },
            modifier = Modifier.size(width = 375.dp, height = 667.dp)
        )

        // Main Scroll Content
        val analyzeButtonRequester = remember { BringIntoViewRequester() }
        val runningPanelRequester = remember { BringIntoViewRequester() }

        var previousPhotoCount by remember { mutableIntStateOf(photos.size) }
        LaunchedEffect(photos.size) {
            if (photos.size > previousPhotoCount) {
                analyzeButtonRequester.bringIntoView()
            }
            previousPhotoCount = photos.size
        }

        val isRunning = runState is QuickValueRunState.Running || runState is QuickValueRunState.Preparing
        LaunchedEffect(isRunning) {
            if (isRunning) {
                runningPanelRequester.bringIntoView()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(DenimTestTags.SCAN_SCREEN)
                .background(DenimColors.canvas)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Card (Archive Header)
            HeaderCard()

            // 2. Photo Collector
            PhotoCollectorCard(
                photos = photos,
                maxCount = 30,
                hasCamera = hasCamera,
                onAddFromCamera = { viewModel.openCamera() },
                onAddFromPicker = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemovePhoto = { index -> viewModel.removePhotoAt(index) },
                onRequestClearAll = { viewModel.requestClearAllPhotos() }
            )

            // 3. Execution Action Button
            DenimPrimaryButton(
                text = "가치 확인하기",
                onClick = {
                    hiddenContainerView?.let { container ->
                        viewModel.onStartValuationClicked(container)
                    }
                },
                enabled = photos.isNotEmpty() && !isRunning,
                modifier = Modifier
                    .testTag(DenimTestTags.SCAN_START_BUTTON)
                    .bringIntoViewRequester(analyzeButtonRequester),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            )

            if (photos.isEmpty()) {
                Text(
                    text = "사진 한 장부터 시작할 수 있어요. 원본은 30장까지 담고, 가장 선명한 사진을 골라 분석합니다.",
                    style = DenimTypography.caption.copy(color = DenimColors.inkSoft),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DenimTestTags.SCAN_EMPTY_HINT)
                )
            }

            // 4. Running Panel
            if (runState is QuickValueRunState.Running) {
                val state = runState as QuickValueRunState.Running
                RunningPanel(
                    state = state,
                    onCancel = { viewModel.runner.cancel() },
                    modifier = Modifier.bringIntoViewRequester(runningPanelRequester)
                )
            } else if (runState is QuickValueRunState.Preparing) {
                val state = runState as QuickValueRunState.Preparing
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DenimTestTags.SCAN_RUNNING_PANEL)
                        .bringIntoViewRequester(runningPanelRequester),
                    colors = CardDefaults.cardColors(containerColor = DenimColors.cardSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = DenimColors.indigo,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = state.message,
                            style = DenimTypography.subheadline,
                            modifier = Modifier.testTag(DenimTestTags.SCAN_RUNNING_STATUS_MESSAGE)
                        )
                    }
                }
            }

            // 5. Value Result Card
            if (latestResult != null && runState is QuickValueRunState.Success) {
                QuickValueResultCard(
                    result = latestResult!!,
                    isSaved = isSaved,
                    onSaveToArchive = { viewModel.saveToArchive() },
                    onNextPhotoInstructionClicked = {
                        viewModel.openCamera()
                    },
                    onRestart = { viewModel.resetValuation() }
                )
            }

            // 6. Error and Timeout Panels (per spec 2.1 & 3.7 retry resets to collecting state)
            if (runState is QuickValueRunState.Error) {
                val errorState = runState as QuickValueRunState.Error
                ErrorPanel(
                    message = errorState.message,
                    onRetry = {
                        viewModel.resetValuation()
                    }
                )
            } else if (runState is QuickValueRunState.Timeout) {
                TimeoutPanel(
                    onRetry = {
                        viewModel.resetValuation()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Camera View Overlay
        if (showCamera) {
            CameraCaptureView(
                currentCount = photos.size,
                maxCount = 30,
                onPhotosCaptured = { newBytes ->
                    viewModel.addPhotos(newBytes)
                    viewModel.closeCamera()
                },
                onDismiss = { viewModel.closeCamera() },
                onPhotoLibrarySaveIssue = { viewModel.onPhotoSaveIssue() }
            )
        }

        // Login Sheet Overlay
        if (showLoginSheet) {
            AIBILoginSheet(
                onDismiss = { viewModel.dismissLoginSheet() },
                onLoginSuccess = {
                    hiddenContainerView?.let { viewModel.onLoginSuccess(it) }
                }
            )
        }

        // Visible Browser Sheet Overlay
        if (isVisibleBrowserPresented && runState is QuickValueRunState.Running) {
            val state = runState as QuickValueRunState.Running
            AIBIVisibleBrowserSheet(
                runner = viewModel.runner,
                elapsedSeconds = state.elapsedSeconds,
                onDismiss = {
                    viewModel.runner.cancel()
                },
                onResultImported = {
                    viewModel.runner.currentSession?.dismissVisibleBrowser()
                }
            )
        }

        // Pre-transfer Consent Alert
        if (showConsentDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onConsentCancelled() },
                modifier = Modifier.testTag(DenimTestTags.DIALOG_CONSENT),
                title = {
                    Text(
                        text = "사진 분석을 시작할까요?",
                        style = DenimTypography.title3.copy(color = DenimColors.charcoal)
                    )
                },
                text = {
                    Text(
                        text = "선택한 사진의 사본과 분석 요청이 로그인된 ChatGPT로 전송됩니다. DenimDex 서버에는 남지 않으며, 전송용 사본은 분석 후 폐기됩니다.",
                        style = DenimTypography.body.copy(color = DenimColors.inkSoft)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            hiddenContainerView?.let { viewModel.onConsentAgreed(it) }
                        },
                        modifier = Modifier.testTag(DenimTestTags.DIALOG_CONSENT_CONFIRM_BUTTON)
                    ) {
                        Text(
                            text = "동의하고 시작",
                            style = DenimTypography.headline.copy(color = DenimColors.indigoBright)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onConsentCancelled() },
                        modifier = Modifier.testTag(DenimTestTags.DIALOG_CONSENT_CANCEL_BUTTON)
                    ) {
                        Text(
                            text = "취소",
                            style = DenimTypography.body.copy(color = DenimColors.inkSoft)
                        )
                    }
                }
            )
        }

        // Clear All Confirmation Dialog
        if (showClearAllConfirm) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissClearAllConfirm() },
                modifier = Modifier.testTag(DenimTestTags.DIALOG_CLEAR_ALL_PHOTOS),
                title = {
                    Text(
                        text = "담은 사진을 모두 비울까요?",
                        style = DenimTypography.title3.copy(color = DenimColors.charcoal)
                    )
                },
                text = {
                    Text(
                        text = "현재 감정을 위해 담은 사진만 비워집니다. 사진 앱과 아카이브의 원본은 그대로 유지됩니다.",
                        style = DenimTypography.body.copy(color = DenimColors.inkSoft)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.clearAllPhotos() },
                        modifier = Modifier.testTag(DenimTestTags.DIALOG_CLEAR_ALL_CONFIRM_BUTTON)
                    ) {
                        Text(
                            text = "모두 비우기",
                            style = DenimTypography.headline.copy(color = DenimColors.signalRed)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissClearAllConfirm() },
                        modifier = Modifier.testTag(DenimTestTags.DIALOG_CLEAR_ALL_CANCEL_BUTTON)
                    ) {
                        Text(
                            text = "취소",
                            style = DenimTypography.body.copy(color = DenimColors.inkSoft)
                        )
                    }
                }
            )
        }

        // Photo Save Issue Alert
        if (showPhotoSaveAlert) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPhotoSaveAlert() },
                modifier = Modifier.testTag(DenimTestTags.DIALOG_PHOTO_SAVE_ALERT),
                title = {
                    Text(
                        text = "사진 앱에 저장하지 못했어요",
                        style = DenimTypography.title3.copy(color = DenimColors.charcoal)
                    )
                },
                text = {
                    Text(
                        text = "촬영한 사진은 감정 목록에 담겼지만 사진 앱에는 저장되지 않았어요. 설정에서 사진 추가 권한을 확인해주세요.",
                        style = DenimTypography.body.copy(color = DenimColors.inkSoft)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.dismissPhotoSaveAlert() },
                        modifier = Modifier.testTag(DenimTestTags.DIALOG_PHOTO_SAVE_CONFIRM_BUTTON)
                    ) {
                        Text("확인", style = DenimTypography.headline.copy(color = DenimColors.indigoBright))
                    }
                }
            )
        }
    }
}

@Composable
private fun HeaderCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.SCAN_HEADER_CARD)
            .shadow(
                elevation = 14.dp,
                shape = DenimShapes.hero,
                ambientColor = DenimColors.indigoDeep.copy(alpha = 0.35f),
                spotColor = DenimColors.indigoDeep.copy(alpha = 0.35f)
            )
            .clip(DenimShapes.hero)
            .background(brush = DenimColors.indigoGradient)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DenimEyebrow("DenimDex · Private Archive", color = DenimColors.brass)
                Text(
                    text = "EST. 2026",
                    style = DenimTypography.captionBold.copy(color = Color.White.copy(alpha = 0.5f))
                )
            }

            Text(
                text = "당신의 데님,\n가치를 발견하다",
                style = DenimTypography.title1.copy(
                    fontSize = 30.sp,
                    color = Color.White,
                    lineHeight = 36.sp
                )
            )

            Column {
                Text(
                    text = "제품의 정체와 한·일 시장 가치를 한 번에 살펴보세요.",
                    style = DenimTypography.caption.copy(color = Color.White.copy(alpha = 0.68f))
                )
                // Brass decorative underline, flush beneath the closing line of the subtitle
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .width(64.dp)
                        .height(2.dp)
                        .background(DenimColors.brass.copy(alpha = 0.85f))
                )
            }
        }
    }
}

@Composable
private fun PhotoCollectorCard(
    photos: List<ByteArray>,
    maxCount: Int = 30,
    hasCamera: Boolean = true,
    onAddFromCamera: () -> Unit,
    onAddFromPicker: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onRequestClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.SCAN_PHOTO_COLLECTOR)
            .denimCard(padding = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "감정 사진", style = DenimTypography.title3)
                Text(text = "실루엣부터 라벨과 작은 각인까지", style = DenimTypography.caption)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Count Badge
                Box(
                    modifier = Modifier
                        .testTag(DenimTestTags.SCAN_PHOTO_COUNT_BADGE)
                        .clip(DenimShapes.pill)
                        .background(DenimColors.fadedDenim)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${photos.size} / $maxCount",
                        style = DenimTypography.captionBold.copy(color = DenimColors.indigo)
                    )
                }

                if (photos.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onRequestClearAll,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag(DenimTestTags.SCAN_CLEAR_ALL_PHOTOS_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "모두 비우기",
                            tint = DenimColors.signalRed
                        )
                    }
                }
            }
        }

        Text(
            text = "사진을 길게 눌러 중요도 순으로 정리할 수 있어요.",
            style = DenimTypography.caption2.copy(color = DenimColors.inkSoft)
        )

        // Photo Grid (3 columns)
        val gridItemsCount = if (photos.size < maxCount) photos.size + 1 else photos.size
        val rows = (gridItemsCount + 2) / 3

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DenimTestTags.SCAN_PHOTO_GRID),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (c in 0 until 3) {
                        val index = r * 3 + c
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            if (index < photos.size) {
                                PhotoThumbnail(
                                    bytes = photos[index],
                                    index = index,
                                    onDelete = { onRemovePhoto(index) }
                                )
                            } else if (index == photos.size && photos.size < maxCount) {
                                AddPhotoTile(
                                    onAddFromCamera = onAddFromCamera,
                                    onAddFromPicker = onAddFromPicker
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Button Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasCamera) {
                DenimSecondaryButton(
                    text = "직접 촬영",
                    onClick = onAddFromCamera,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(DenimTestTags.SCAN_CAMERA_BUTTON),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = DenimColors.indigoDeep
                        )
                    }
                )
            }

            DenimSecondaryButton(
                text = "사진 선택",
                onClick = onAddFromPicker,
                modifier = Modifier
                    .weight(1f)
                    .testTag(DenimTestTags.SCAN_PHOTO_PICKER_BUTTON),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = DenimColors.indigoDeep
                    )
                }
            )
        }
    }
}

@Composable
private fun PhotoThumbnail(
    bytes: ByteArray,
    index: Int,
    onDelete: () -> Unit
) {
    val bitmap = remember(bytes) {
        try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(DenimTestTags.scanPhotoThumbnail(index))
            .clip(DenimShapes.tile)
            .background(DenimColors.offWhite)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "사진 ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Drag handle indicator on bottom-left
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .size(16.dp)
        )

        // Delete button on top-right
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .testTag(DenimTestTags.scanPhotoDeleteButton(index))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "삭제",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun AddPhotoTile(
    onAddFromCamera: () -> Unit,
    onAddFromPicker: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(DenimTestTags.SCAN_ADD_PHOTO_TILE)
            .clip(DenimShapes.tile)
            .background(DenimColors.offWhite)
            .border(1.dp, DenimColors.hairline, DenimShapes.tile)
            .clickable { onAddFromPicker() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "사진 추가",
                tint = DenimColors.indigoBright,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "사진 추가",
                style = DenimTypography.caption.copy(color = DenimColors.indigoBright)
            )
        }
    }
}

@Composable
private fun RunningPanel(
    state: QuickValueRunState.Running,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingSec = state.elapsedSeconds?.let { CountdownFormatter.remainingSeconds(it) }
    val fraction = state.elapsedSeconds?.let { CountdownFormatter.progressFraction(it) }
    val formattedTime = remainingSec?.let { CountdownFormatter.formatMinutesSeconds(it) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.SCAN_RUNNING_PANEL)
            .denimCard(padding = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = DenimColors.indigo,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = state.statusMessage,
                    style = DenimTypography.subheadline.copy(color = DenimColors.charcoal),
                    modifier = Modifier.testTag(DenimTestTags.SCAN_RUNNING_STATUS_MESSAGE)
                )
            }

            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(DenimTestTags.SCAN_RUNNING_CANCEL_BUTTON)
            ) {
                Text(text = "취소", style = DenimTypography.captionBold.copy(color = DenimColors.signalRed))
            }
        }

        // Countdown row and progress bar (rendered only after generation/stabilization starts)
        if (state.elapsedSeconds != null && formattedTime != null && fraction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "남은 시간 $formattedTime",
                    style = DenimTypography.captionBold.copy(color = DenimColors.indigoBright),
                    modifier = Modifier.testTag(DenimTestTags.SCAN_RUNNING_COUNTDOWN)
                )
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = DenimColors.indigo,
                trackColor = DenimColors.fadedDenim
            )
        }

        // Photo count summary
        Text(
            text = "${state.sentPhotoCount}장 분석 · 유사 사진 ${state.excludedSimilarCount}장 제외 · 전송 한도 ${state.excludedLimitCount}장 제외",
            style = DenimTypography.caption2.copy(color = DenimColors.inkSoft),
            modifier = Modifier.testTag(DenimTestTags.SCAN_RUNNING_PHOTO_SUMMARY)
        )
    }
}

@Composable
private fun ErrorPanel(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.SCAN_ERROR_PANEL)
            .denimCard(padding = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = DenimColors.signalRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = DenimTypography.subheadline.copy(color = DenimColors.signalRed),
                modifier = Modifier.testTag(DenimTestTags.SCAN_ERROR_MESSAGE)
            )
        }

        DenimSecondaryButton(
            text = "다시 시도",
            onClick = onRetry,
            modifier = Modifier.testTag(DenimTestTags.SCAN_ERROR_RETRY_BUTTON)
        )
    }
}

@Composable
private fun TimeoutPanel(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DenimTestTags.SCAN_TIMEOUT_PANEL)
            .denimCard(padding = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = DenimColors.signalRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "분석이 예상보다 오래 걸리고 있어요. 잠시 후 다시 시도해주세요.",
                style = DenimTypography.subheadline.copy(color = DenimColors.charcoal)
            )
        }

        DenimSecondaryButton(
            text = "다시 시도",
            onClick = onRetry,
            modifier = Modifier.testTag(DenimTestTags.SCAN_TIMEOUT_RETRY_BUTTON)
        )
    }
}
