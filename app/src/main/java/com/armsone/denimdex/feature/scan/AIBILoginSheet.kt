package com.armsone.denimdex.feature.scan

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.armsone.denimdex.core.design.DenimColors
import com.armsone.denimdex.core.design.DenimTheme
import com.armsone.denimdex.core.design.DenimTypography

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBILoginSheet(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var isVerified by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = DenimColors.canvas
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ChatGPT 로그인",
                    style = DenimTypography.title3.copy(color = DenimColors.charcoal)
                )

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "닫기",
                        style = DenimTypography.subheadline.copy(
                            color = DenimColors.indigoBright,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Description banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isVerified) DenimColors.successGreen.copy(alpha = 0.12f) else DenimColors.fadedDenim)
                    .padding(12.dp)
            ) {
                Text(
                    text = if (isVerified) "로그인을 확인했어요" else "ChatGPT에 로그인해주세요. 로그인이 확인되면 이 창이 자동으로 닫히고 가치 분석을 계속합니다.",
                    style = DenimTypography.caption.copy(
                        color = if (isVerified) DenimColors.successGreen else DenimColors.inkSoft,
                        fontWeight = if (isVerified) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }

            // Browser WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.setSupportMultipleWindows(true)

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Probe for profile / user menu presence indicating successful sign-in
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        const userMenu = document.querySelector("button[data-testid='profile-button'], button[data-testid='user-menu-button'], button[data-testid='user-menu'], button[data-testid='accounts-profile-button']");
                                        const promptArea = document.querySelector("#prompt-textarea, textarea[data-id='root']");
                                        return (userMenu !== null || promptArea !== null) ? 'logged_in' : 'not_yet';
                                    })()
                                    """.trimIndent()
                                ) { result ->
                                    val clean = result?.replace("\"", "")?.trim()
                                    if (clean == "logged_in") {
                                        isVerified = true
                                        onLoginSuccess()
                                    }
                                }
                            }
                        }

                        loadUrl("https://chatgpt.com/")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
