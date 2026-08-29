package com.armsone.denimdex.feature.scan

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.armsone.denimdex.core.aibi.AIBILoginClassifier
import com.armsone.denimdex.core.design.DenimColors
import com.armsone.denimdex.core.design.DenimTypography
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBILoginSheet(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var isVerified by remember { mutableStateOf(false) }
    val isCompleted = remember { AtomicBoolean(false) }
    val coroutineScope = rememberCoroutineScope()
    var pollingJob by remember { mutableStateOf<Job?>(null) }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            pollingJob?.cancel()
            pollingJob = null
            activeWebView?.apply {
                stopLoading()
                webViewClient = WebViewClient()
                destroy()
            }
            activeWebView = null
        }
    }

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

                TextButton(onClick = {
                    pollingJob?.cancel()
                    pollingJob = null
                    onDismiss()
                }) {
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
                        activeWebView = this
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

                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        val checkScript = AIBILoginClassifier.buildProbeScript()

                        fun checkPositiveAuthentication() {
                            if (isCompleted.get() || !AIBILoginClassifier.canInspectUrl(url)) return
                            evaluateJavascript(checkScript) { result ->
                                if (AIBILoginClassifier.parseProbeResult(result) == com.armsone.denimdex.core.aibi.LoginStatus.LOGGED_IN) {
                                    if (isCompleted.compareAndSet(false, true)) {
                                        pollingJob?.cancel()
                                        pollingJob = null
                                        isVerified = true
                                        CookieManager.getInstance().flush()
                                        onLoginSuccess()
                                    }
                                }
                            }
                        }

                        fun startHydrationPolling() {
                            if (isCompleted.get()) return
                            pollingJob?.cancel()
                            pollingJob = coroutineScope.launch {
                                val deadline = System.currentTimeMillis() + 20_000L
                                while (isActive && !isCompleted.get() && System.currentTimeMillis() < deadline) {
                                    checkPositiveAuthentication()
                                    delay(500L)
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (AIBILoginClassifier.canInspectUrl(url)) {
                                    checkPositiveAuthentication()
                                    startHydrationPolling()
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
