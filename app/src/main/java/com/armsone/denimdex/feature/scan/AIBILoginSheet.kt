package com.armsone.denimdex.feature.scan

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Message
import android.view.MotionEvent
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.armsone.denimdex.core.aibi.AIBILoginClassifier
import com.armsone.denimdex.core.aibi.AIBIProviderRegistry
import com.armsone.denimdex.core.design.DenimColors
import com.armsone.denimdex.core.design.DenimTypography
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean

private fun effectivePort(uri: Uri): Int =
    if (uri.port != -1) uri.port else if (uri.scheme.equals("https", ignoreCase = true)) 443 else -1

private fun isAllowedHttpsOrigin(url: Uri?, origins: List<String>): Boolean {
    if (url == null || !url.scheme.equals("https", ignoreCase = true) || url.host.isNullOrEmpty()) return false
    return origins.any { candidate ->
        val allowed = Uri.parse(candidate)
        allowed.scheme.equals("https", ignoreCase = true) &&
            url.host.equals(allowed.host, ignoreCase = true) &&
            effectivePort(url) == effectivePort(allowed)
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBILoginSheet(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var isVerified by remember { mutableStateOf(false) }
    val isCompleted = remember { AtomicBoolean(false) }
    val rendererLossHandled = remember { AtomicBoolean(false) }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pollingJob by remember { mutableStateOf<Job?>(null) }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    var authChild by remember { mutableStateOf<WebView?>(null) }
    var authHost by remember { mutableStateOf<String?>(null) }
    val afterAuthClosed = remember { arrayOfNulls<() -> Unit>(1) }

    fun closeAuthChild(recheck: Boolean) {
        val child = authChild ?: return
        authChild = null
        authHost = null
        child.stopLoading()
        child.webChromeClient = WebChromeClient()
        child.webViewClient = WebViewClient()
        (child.parent as? ViewGroup)?.removeView(child)
        child.destroy()
        activeWebView?.let { parent ->
            parent.post {
                if (activeWebView === parent && !isCompleted.get() && !rendererLossHandled.get() && authChild == null) {
                    parent.requestFocus()
                }
            }
        }
        if (recheck && !isCompleted.get() && !rendererLossHandled.get()) afterAuthClosed[0]?.invoke()
    }

    fun stopAuthWork() {
        isCompleted.set(true)
        pollingJob?.cancel()
        pollingJob = null
        afterAuthClosed[0] = null
    }

    fun shutdownAfterRendererLoss() {
        if (!rendererLossHandled.compareAndSet(false, true)) return
        val alreadyFinished = isCompleted.get()
        stopAuthWork()
        authChild?.let { child ->
            authChild = null
            authHost = null
            child.webChromeClient = WebChromeClient()
            child.webViewClient = WebViewClient()
            (child.parent as? ViewGroup)?.removeView(child)
            child.destroy()
        }
        activeWebView?.let { web ->
            activeWebView = null
            web.setOnTouchListener(null)
            web.onFocusChangeListener = null
            web.webChromeClient = WebChromeClient()
            web.webViewClient = WebViewClient()
            (web.parent as? ViewGroup)?.removeView(web)
            web.destroy()
        }
        if (!alreadyFinished) onDismiss()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !isCompleted.get() && !rendererLossHandled.get() && authChild == null) {
                afterAuthClosed[0]?.invoke()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopAuthWork()
            closeAuthChild(recheck = false)
            activeWebView?.let { web ->
                activeWebView = null
                web.setOnTouchListener(null)
                web.onFocusChangeListener = null
                web.stopLoading()
                web.webChromeClient = WebChromeClient()
                web.webViewClient = WebViewClient()
                (web.parent as? ViewGroup)?.removeView(web)
                web.destroy()
            }
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
                    stopAuthWork()
                    closeAuthChild(recheck = false)
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

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Browser WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        val parentWeb = this
                        activeWebView = parentWeb
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

                        isFocusable = true
                        isFocusableInTouchMode = true

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        val providerConfig = AIBIProviderRegistry(ctx.applicationContext).getProviderConfig()
                        val scriptOrigins = providerConfig.allowedScriptOrigins
                        val authOrigins = providerConfig.allowedAuthOrigins

                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        val checkScript = AIBILoginClassifier.buildProbeScript()

                        fun canInspectCurrentOrigin(candidateUrl: String? = url): Boolean {
                            if (!AIBILoginClassifier.canInspectUrl(candidateUrl)) return false
                            val parsed = candidateUrl?.let(Uri::parse) ?: return false
                            return isAllowedHttpsOrigin(parsed, scriptOrigins)
                        }

                        fun checkPositiveAuthentication() {
                            if (isCompleted.get() || rendererLossHandled.get() || activeWebView !== parentWeb || authChild != null || !canInspectCurrentOrigin()) return
                            evaluateJavascript(checkScript) { result ->
                                if (isCompleted.get() || rendererLossHandled.get() || activeWebView !== parentWeb || authChild != null || !canInspectCurrentOrigin()) return@evaluateJavascript
                                if (AIBILoginClassifier.parseProbeResult(result) == com.armsone.denimdex.core.aibi.LoginStatus.LOGGED_IN) {
                                    if (isCompleted.compareAndSet(false, true)) {
                                        pollingJob?.cancel()
                                        pollingJob = null
                                        afterAuthClosed[0] = null
                                        isVerified = true
                                        closeAuthChild(recheck = false)
                                        CookieManager.getInstance().flush()
                                        onLoginSuccess()
                                    }
                                }
                            }
                        }

                        fun startHydrationPolling(windowMs: Long = 20_000L) {
                            if (isCompleted.get() || rendererLossHandled.get() || activeWebView !== parentWeb) return
                            pollingJob?.cancel()
                            pollingJob = coroutineScope.launch {
                                val deadline = System.currentTimeMillis() + windowMs
                                while (isActive && !isCompleted.get() && !rendererLossHandled.get() && activeWebView === parentWeb && System.currentTimeMillis() < deadline) {
                                    if (authChild == null && canInspectCurrentOrigin()) {
                                        checkPositiveAuthentication()
                                    }
                                    delay(500L)
                                }
                            }
                        }

                        fun rearmBoundedRecheck(windowMs: Long = 20_000L) {
                            if (isCompleted.get() || rendererLossHandled.get() || activeWebView !== parentWeb || authChild != null) return
                            if (canInspectCurrentOrigin()) {
                                checkPositiveAuthentication()
                            }
                            startHydrationPolling(windowMs)
                        }

                        afterAuthClosed[0] = {
                            rearmBoundedRecheck(20_000L)
                        }

                        setOnTouchListener { _, event ->
                            if (event?.actionMasked == MotionEvent.ACTION_UP) {
                                rearmBoundedRecheck(10_000L)
                            }
                            false
                        }

                        setOnFocusChangeListener { _, hasFocus ->
                            if (hasFocus) {
                                rearmBoundedRecheck(10_000L)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                if (!isUserGesture || view !== this@apply || authChild != null || isCompleted.get()) return false
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                val child = WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.setSupportMultipleWindows(false)
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onCreateWindow(
                                            view: WebView?,
                                            isDialog: Boolean,
                                            isUserGesture: Boolean,
                                            resultMsg: Message?
                                        ): Boolean = false

                                        override fun onCloseWindow(window: WebView?) {
                                            if (window === authChild) closeAuthChild(recheck = true)
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        private fun permitted(u: Uri?): Boolean =
                                            isAllowedHttpsOrigin(u, scriptOrigins) || isAllowedHttpsOrigin(u, authOrigins)

                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            val u = request?.url ?: return true
                                            if (u.toString() == "about:blank") return false
                                            return !permitted(u)
                                        }

                                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                            val req = request ?: return null
                                            if (!req.isForMainFrame || req.url.toString() == "about:blank" || permitted(req.url)) return null
                                            return WebResourceResponse(
                                                "text/plain", "UTF-8", 403, "Forbidden",
                                                emptyMap(), ByteArrayInputStream(ByteArray(0))
                                            )
                                        }

                                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                            val u = url?.let(Uri::parse)
                                            if (u?.toString() == "about:blank") return
                                            if (view === authChild && !permitted(u)) {
                                                closeAuthChild(recheck = true)
                                            } else if (view === authChild) {
                                                authHost = u?.host
                                            }
                                        }

                                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                            shutdownAfterRendererLoss()
                                            return true
                                        }
                                    }
                                }
                                authChild = child
                                transport.webView = child
                                resultMsg.sendToTarget()
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val u = request?.url ?: return true
                                return !(isAllowedHttpsOrigin(u, scriptOrigins) || isAllowedHttpsOrigin(u, authOrigins))
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                shutdownAfterRendererLoss()
                                return true
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                if (canInspectCurrentOrigin(url)) {
                                    rearmBoundedRecheck(20_000L)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (canInspectCurrentOrigin(url)) {
                                    rearmBoundedRecheck(20_000L)
                                }
                            }
                        }

                        post {
                            if (activeWebView === parentWeb && !isCompleted.get() && authChild == null) {
                                parentWeb.requestFocus()
                            }
                        }

                        loadUrl("https://chatgpt.com/")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            authChild?.let { child ->
                Column(modifier = Modifier.fillMaxSize().background(DenimColors.canvas)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "인증 창: ${authHost ?: "확인 중"}",
                            style = DenimTypography.caption.copy(color = DenimColors.inkSoft),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { closeAuthChild(recheck = true) }) {
                            Text(
                                text = "인증 창 닫기",
                                style = DenimTypography.subheadline.copy(
                                    color = DenimColors.indigoBright,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                    key(child) {
                        AndroidView(
                            factory = {
                                child.also { mounted ->
                                    mounted.post {
                                        if (authChild === mounted && !isCompleted.get()) mounted.requestFocus()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
            }
            }
        }
    }
}
