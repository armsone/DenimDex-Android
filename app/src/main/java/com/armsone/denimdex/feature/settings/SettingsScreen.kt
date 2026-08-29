package com.armsone.denimdex.feature.settings

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.armsone.denimdex.BuildConfig
import com.armsone.denimdex.core.aibi.LoginStatus
import com.armsone.denimdex.core.design.*
import com.armsone.denimdex.feature.scan.AIBILoginSheet

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val loginStatus by viewModel.loginStatus.collectAsState()
    val showLoginSheet by viewModel.showLoginSheet.collectAsState()
    val showClearSessionConfirm by viewModel.showClearSessionConfirm.collectAsState()
    val showSessionClearedAlert by viewModel.showSessionClearedAlert.collectAsState()
    val showClearArchiveConfirm by viewModel.showClearArchiveConfirm.collectAsState()
    val archiveCount by viewModel.archiveCount.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        // A real attached reference viewport is required for ChatGPT's mobile SPA to hydrate
        // its account controls. It remains behind the opaque settings surface.
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(375, 667)
                    alpha = 0.001f
                    isClickable = false
                    isFocusable = false
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    post { viewModel.refreshLoginStatus(this) }
                }
            },
            modifier = Modifier.size(width = 375.dp, height = 667.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(DenimTestTags.SETTINGS_SCREEN)
                .background(DenimColors.canvas)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "설정",
                style = DenimTypography.largeTitle,
                modifier = Modifier.testTag(DenimTestTags.SETTINGS_TITLE)
            )

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DenimTestTags.SETTINGS_HEADER_BANNER)
                    .clip(RoundedCornerShape(20.dp))
                    .background(brush = DenimColors.indigoGradient)
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DenimEyebrow("Privacy & Collection", color = DenimColors.washedDenim)
                    Text(
                        text = "당신의 기록을\n안전하게 관리합니다",
                        style = DenimTypography.title1.copy(fontSize = 26.sp, color = Color.White, lineHeight = 32.sp)
                    )
                }
            }

            // Section: ChatGPT connection
            SettingsSection(title = "ChatGPT 연결") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DenimTestTags.SETTINGS_CHATGPT_ROW)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.openLoginSheet() }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "ChatGPT", style = DenimTypography.body.copy(color = DenimColors.charcoal))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .testTag(DenimTestTags.SETTINGS_CHATGPT_STATUS_BADGE)
                            .clip(RoundedCornerShape(10.dp))
                            .background(loginStatus.color.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when (loginStatus) {
                                LoginStatus.CHECKING -> Icons.Default.Sync
                                LoginStatus.LOGGED_IN -> Icons.Default.CheckCircle
                                LoginStatus.LOGIN_REQUIRED -> Icons.Default.ErrorOutline
                                LoginStatus.UNKNOWN -> Icons.Default.HelpOutline
                            },
                            contentDescription = null,
                            tint = loginStatus.color,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = loginStatus.title,
                            style = DenimTypography.captionBold.copy(color = loginStatus.color)
                        )
                    }
                }

                TextButton(
                    onClick = { viewModel.requestClearSession() },
                    modifier = Modifier
                        .align(Alignment.Start)
                        .testTag(DenimTestTags.SETTINGS_CLEAR_SESSION_BUTTON)
                ) {
                    Text(
                        text = "ChatGPT 로그인 정보 지우기",
                        style = DenimTypography.captionBold.copy(color = DenimColors.signalRed)
                    )
                }

                Text(
                    text = "로그인은 ChatGPT 공식 화면에서만 진행됩니다. DenimDex는 비밀번호와 로그인 쿠키를 읽거나 별도로 저장하지 않습니다.",
                    style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                )
            }

            // Section: Archive sync
            SettingsSection(
                title = "아카이브 동기화",
                modifier = Modifier.testTag(DenimTestTags.SETTINGS_SYNC_SECTION)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "연결 상태", style = DenimTypography.body.copy(color = DenimColors.charcoal))
                    Text(text = "연결되지 않음", style = DenimTypography.captionBold.copy(color = DenimColors.inkSoft))
                }
                Text(
                    text = "현재는 이 기기의 개인 아카이브만 사용합니다. 개인 NAS 연결은 준비 중입니다.",
                    style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                )
            }

            // Section: Photos & privacy
            SettingsSection(
                title = "사진과 개인정보",
                modifier = Modifier.testTag(DenimTestTags.SETTINGS_PRIVACY_SECTION)
            ) {
                Text(
                    text = "원본 사진은 기본적으로 이 기기에만 보관됩니다. 감정을 시작하면 사진 사본과 분석 요청이 로그인된 ChatGPT로 전송되며, 전송용 사본은 작업 후 폐기됩니다.",
                    style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                )
            }

            // Section: Archive management
            SettingsSection(title = "아카이브 관리") {
                OutlinedButton(
                    onClick = { viewModel.requestClearArchive() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DenimTestTags.SETTINGS_CLEAR_ARCHIVE_BUTTON),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DenimColors.signalRed)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("내 아카이브 전체 삭제")
                }
                Text(
                    text = "보관 중인 데님 기록 ${archiveCount}개가 모두 삭제되며 되돌릴 수 없습니다.",
                    style = DenimTypography.caption.copy(color = DenimColors.inkSoft)
                )
            }

            // Section: Version
            SettingsSection(
                title = "버전 정보",
                modifier = Modifier.testTag(DenimTestTags.SETTINGS_VERSION_SECTION)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "현재 버전", style = DenimTypography.body.copy(color = DenimColors.charcoal))
                    Text(
                        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.SOURCE_DISPLAY_BUILD})",
                        style = DenimTypography.captionBold.copy(color = DenimColors.inkSoft),
                        modifier = Modifier.testTag(DenimTestTags.SETTINGS_VERSION_VALUE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showLoginSheet) {
            AIBILoginSheet(
                onDismiss = { viewModel.dismissLoginSheet() },
                onLoginSuccess = { viewModel.onLoginSuccess() }
            )
        }

        if (showClearSessionConfirm) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissClearSessionConfirm() },
                modifier = Modifier.testTag(DenimTestTags.DIALOG_CLEAR_SESSION),
                title = {
                    Text(
                        text = "ChatGPT 로그인 정보를 지울까요?",
                        style = DenimTypography.title3.copy(color = DenimColors.charcoal)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmClearSession() },
                        modifier = Modifier.testTag(DenimTestTags.DIALOG_CLEAR_SESSION_CONFIRM_BUTTON)
                    ) {
                        Text("지우기", style = DenimTypography.headline.copy(color = DenimColors.signalRed))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissClearSessionConfirm() },
                        modifier = Modifier.testTag(DenimTestTags.DIALOG_CLEAR_SESSION_CANCEL_BUTTON)
                    ) {
                        Text("취소", style = DenimTypography.body.copy(color = DenimColors.inkSoft))
                    }
                }
            )
        }

        if (showSessionClearedAlert) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSessionClearedAlert() },
                modifier = Modifier.testTag(DenimTestTags.DIALOG_SESSION_CLEARED),
                title = {
                    Text(
                        text = "ChatGPT 로그인 정보를 지웠어요",
                        style = DenimTypography.title3.copy(color = DenimColors.charcoal)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.dismissSessionClearedAlert() },
                        modifier = Modifier.testTag(DenimTestTags.DIALOG_SESSION_CLEARED_CONFIRM_BUTTON)
                    ) {
                        Text("확인", style = DenimTypography.headline.copy(color = DenimColors.indigoBright))
                    }
                }
            )
        }

        if (showClearArchiveConfirm) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissClearArchiveConfirm() },
                modifier = Modifier.testTag(DenimTestTags.DIALOG_CLEAR_ARCHIVE),
                title = {
                    Text(
                        text = "내 아카이브 전체 삭제",
                        style = DenimTypography.title3.copy(color = DenimColors.charcoal)
                    )
                },
                text = {
                    Text(
                        text = "보관 중인 데님 기록 ${archiveCount}개가 모두 삭제되며 되돌릴 수 없습니다.",
                        style = DenimTypography.body.copy(color = DenimColors.inkSoft)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmClearArchive() },
                        modifier = Modifier.testTag(DenimTestTags.DIALOG_CLEAR_ARCHIVE_CONFIRM_BUTTON)
                    ) {
                        Text("전체 삭제", style = DenimTypography.headline.copy(color = DenimColors.signalRed))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissClearArchiveConfirm() },
                        modifier = Modifier.testTag(DenimTestTags.DIALOG_CLEAR_ARCHIVE_CANCEL_BUTTON)
                    ) {
                        Text("취소", style = DenimTypography.body.copy(color = DenimColors.inkSoft))
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .denimCard(padding = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = title, style = DenimTypography.headline.copy(color = DenimColors.charcoal))
        content()
    }
}
