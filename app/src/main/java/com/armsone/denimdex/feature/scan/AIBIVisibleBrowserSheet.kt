package com.armsone.denimdex.feature.scan

import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.armsone.denimdex.core.aibi.AIBISession
import com.armsone.denimdex.core.design.DenimColors
import com.armsone.denimdex.core.design.DenimPrimaryButton
import com.armsone.denimdex.core.design.DenimSecondaryButton
import com.armsone.denimdex.core.design.DenimTypography
import com.armsone.denimdex.core.domain.CountdownFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBIVisibleBrowserSheet(
    runner: QuickValueRunner,
    elapsedSeconds: Double,
    onDismiss: () -> Unit,
    onResultImported: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showPasteModal by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var pasteError by remember { mutableStateOf<String?>(null) }

    val remainingSec = CountdownFormatter.remainingSeconds(elapsedSeconds)
    val fraction = CountdownFormatter.progressFraction(elapsedSeconds)
    val formattedTime = CountdownFormatter.formatMinutesSeconds(remainingSec)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = DenimColors.canvas
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "취소",
                        style = DenimTypography.subheadline.copy(color = DenimColors.signalRed)
                    )
                }

                Text(
                    text = "ChatGPT",
                    style = DenimTypography.headline.copy(color = DenimColors.charcoal)
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "메뉴",
                            tint = DenimColors.charcoal
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("문구 복사", style = DenimTypography.body) },
                            onClick = {
                                showMenu = false
                                runner.manualCopyPrompt()
                                Toast.makeText(context, "프롬프트를 클립보드에 복사했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("결과 붙여넣기", style = DenimTypography.body) },
                            onClick = {
                                showMenu = false
                                pasteText = ""
                                pasteError = null
                                showPasteModal = true
                            }
                        )
                    }
                }
            }

            // Progress & Countdown row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "남은 시간 $formattedTime",
                        style = DenimTypography.captionBold.copy(color = DenimColors.indigoBright)
                    )
                    Text(
                        text = "로그인/보안 확인 후 자동으로 계속됩니다",
                        style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = DenimColors.indigo,
                    trackColor = DenimColors.fadedDenim,
                )
            }

            // Visible WebView
            val visibleView = runner.currentSession?.visibleWebView
            if (visibleView != null) {
                AndroidView(
                    factory = {
                        (visibleView.parent as? ViewGroup)?.removeView(visibleView)
                        visibleView
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DenimColors.indigo)
                }
            }
        }
    }

    // Manual Paste Modal
    if (showPasteModal) {
        AlertDialog(
            onDismissRequest = { showPasteModal = false },
            title = {
                Text(
                    text = "결과 붙여넣기",
                    style = DenimTypography.title3.copy(color = DenimColors.charcoal)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ChatGPT 화면에서 답변을 복사해 아래에 붙여넣으세요.",
                        style = DenimTypography.body.copy(color = DenimColors.inkSoft)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = {
                            pasteText = it
                            pasteError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp),
                        placeholder = { Text("```json\n{\n  \"schemaVersion\": 2...\n}") },
                        textStyle = DenimTypography.caption.copy(color = DenimColors.charcoal)
                    )
                    if (pasteError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pasteError ?: "",
                            style = DenimTypography.caption.copy(color = DenimColors.signalRed)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val success = runner.manualImportResultText(pasteText) {
                            onResultImported()
                        }
                        if (success) {
                            showPasteModal = false
                        } else {
                            pasteError = "올바른 Quick Value JSON 결과를 찾지 못했습니다."
                        }
                    }
                ) {
                    Text("이 내용으로 가져오기", style = DenimTypography.headline.copy(color = DenimColors.indigoBright))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteModal = false }) {
                    Text("닫기", style = DenimTypography.body.copy(color = DenimColors.inkSoft))
                }
            }
        )
    }
}
