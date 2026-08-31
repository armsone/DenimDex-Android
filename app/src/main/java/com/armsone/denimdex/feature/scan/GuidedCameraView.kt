package com.armsone.denimdex.feature.scan

import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.armsone.denimdex.core.design.DenimColors
import com.armsone.denimdex.core.design.DenimTestTags
import com.armsone.denimdex.core.design.DenimTypography
import java.util.concurrent.Executors

/**
 * Full-screen guided camera stepping through the nine pants/jacket slots.
 * Shows step count, title, short instruction and a small real reference image
 * (tap to enlarge, tap again to dismiss); bottom bar keeps previous on the left,
 * the 72dp shutter centered and skip on the right without ever overlapping.
 */
@Composable
fun GuidedCameraView(
    steps: List<GuidedCaptureStep>,
    slots: List<GuidedSlotState>,
    startIndex: Int,
    onCaptured: (Int, ByteArray) -> Unit,
    onSkip: (Int) -> Unit,
    onDismiss: () -> Unit,
    onPhotoLibrarySaveIssue: () -> Unit
) {
    if (steps.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentIndex by remember { mutableIntStateOf(startIndex.coerceIn(0, steps.lastIndex)) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var showReferencePreview by remember { mutableStateOf(false) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    val step = steps[currentIndex]
    val capturedCount = slots.count { it.isCaptured }

    fun advanceOrFinish() {
        if (currentIndex < steps.lastIndex) {
            currentIndex += 1
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(DenimTestTags.GUIDED_CAMERA_SCREEN)
            .background(Color.Black)
    ) {
        // Camera Preview (rebinds when the lens changes)
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { previewView ->
                val boundLens = previewView.tag as? Int
                if (boundLens != lensFacing) {
                    previewView.tag = lensFacing
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setFlashMode(flashMode)
                                .build()
                            imageCapture = capture

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            // Camera unavailable on emulator or TV
                        }
                    }, ContextCompat.getMainExecutor(previewView.context))
                }
                imageCapture?.flashMode = flashMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar: cancel, step count, done
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "취소",
                        style = DenimTypography.subheadline.copy(color = Color.White)
                    )
                }

                Box(
                    modifier = Modifier
                        .testTag(DenimTestTags.GUIDED_CAMERA_STEP_COUNT)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${steps.size}",
                        style = DenimTypography.captionBold.copy(color = Color.White, fontSize = 14.sp)
                    )
                }

                Box(
                    modifier = Modifier
                        .testTag(DenimTestTags.GUIDED_CAMERA_DONE_BUTTON)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (capturedCount > 0) DenimColors.indigoBright else Color.Black.copy(alpha = 0.5f))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "완료",
                        style = DenimTypography.subheadline.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step HUD: title, instruction, small reference image
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${currentIndex + 1}. ${step.title}",
                        style = DenimTypography.headline.copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(DenimTestTags.GUIDED_CAMERA_STEP_TITLE)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = step.instruction,
                        style = DenimTypography.caption.copy(color = Color.White.copy(alpha = 0.85f)),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(DenimTestTags.GUIDED_CAMERA_STEP_INSTRUCTION)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Image(
                    painter = painterResource(step.thumbnailRes),
                    contentDescription = "${step.title} 참고 이미지",
                    modifier = Modifier
                        .size(58.dp)
                        .testTag(DenimTestTags.GUIDED_CAMERA_REFERENCE_IMAGE)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { showReferencePreview = true },
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }

        // Right-edge camera controls: lens flip and flash
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag(DenimTestTags.GUIDED_CAMERA_SWITCH_LENS_BUTTON)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "카메라 전환",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    flashMode = if (flashMode == ImageCapture.FLASH_MODE_OFF) {
                        ImageCapture.FLASH_MODE_ON
                    } else {
                        ImageCapture.FLASH_MODE_OFF
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag(DenimTestTags.GUIDED_CAMERA_FLASH_BUTTON)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = if (flashMode == ImageCapture.FLASH_MODE_ON) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "플래시 토글",
                    tint = if (flashMode == ImageCapture.FLASH_MODE_ON) DenimColors.brass else Color.White
                )
            }
        }

        // Bottom controls: previous (left) / 72dp shutter (center) / skip (right).
        // Weighted side boxes plus fixed 20dp spacers keep skip >= 20dp from the
        // shutter, and the 12dp end padding keeps it >= 12dp from the content edge.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                val previousEnabled = currentIndex > 0
                Box(
                    modifier = Modifier
                        .testTag(DenimTestTags.GUIDED_CAMERA_PREVIOUS_BUTTON)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = if (previousEnabled) 0.5f else 0.25f))
                        .clickable(enabled = previousEnabled) { currentIndex -= 1 }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "이전",
                        style = DenimTypography.subheadline.copy(
                            color = Color.White.copy(alpha = if (previousEnabled) 1f else 0.4f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Shutter Button (72dp circle)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .testTag(DenimTestTags.GUIDED_CAMERA_SHUTTER_BUTTON)
                    .border(5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        val capture = imageCapture ?: return@clickable
                        val stepIndex = currentIndex
                        capture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val buffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    image.close()

                                    ContextCompat.getMainExecutor(context).execute {
                                        onCaptured(stepIndex, bytes)
                                        advanceOrFinish()
                                    }

                                    // Save to public gallery via MediaStore
                                    saveToMediaStore(context, bytes, onPhotoLibrarySaveIssue)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    // Error capturing
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {}

            Spacer(modifier = Modifier.width(20.dp))

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .testTag(DenimTestTags.GUIDED_CAMERA_SKIP_BUTTON)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            onSkip(currentIndex)
                            advanceOrFinish()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "건너뛰기",
                        style = DenimTypography.subheadline.copy(color = Color.White),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // High-resolution reference preview overlay (tap anywhere to dismiss)
        if (showReferencePreview) {
            GuidedReferencePreviewOverlay(
                previewRes = step.previewRes,
                contentDescription = "${step.title} 참고 이미지 확대",
                onDismiss = { showReferencePreview = false }
            )
        }
    }
}
