package com.armsone.denimdex.core.aibi

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.graphics.Color
import com.armsone.denimdex.core.design.DenimColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.net.URI

enum class LoginStatus(val title: String, val iconName: String, val color: Color) {
    CHECKING("확인 중", "arrow.triangle.2.circlepath", DenimColors.inkSoft),
    LOGGED_IN("로그인됨", "checkmark.circle.fill", DenimColors.successGreen),
    LOGIN_REQUIRED("로그인 필요", "exclamationmark.circle.fill", DenimColors.warningAmber),
    UNKNOWN("확인 필요", "questionmark.circle", DenimColors.inkSoft);
}

object AIBILoginClassifier {
    val AUTH_SELECTORS = listOf(
        "button[data-testid='profile-button']",
        "button[data-testid='user-menu-button']",
        "button[data-testid='user-menu']",
        "button[data-testid='accounts-profile-button']",
        "button[aria-label*='profile menu' i]",
        "button[aria-label*='User menu' i]",
        "button[aria-label*='계정' i]",
        "button[aria-label*='프로필' i]",
        "[data-testid='accounts-profile-button']",
        "[data-testid='profile-button']",
        "[data-testid='user-avatar']",
        "a[href*='/settings/account']"
    )

    val LOGIN_SELECTORS = listOf(
        "button[data-testid='login-button']",
        "a[href*='/auth/login']",
        "a[href*='login.openai.com']",
        "a[href*='auth.openai.com']",
        "button[data-testid='signup-button']",
        "button[data-testid='login']",
        "a[href*='/login']"
    )

    val CHALLENGE_SELECTORS = listOf(
        "#cf-challenge-running",
        "iframe[src*='challenges.cloudflare.com']",
        "#challenge-form",
        "#challenge-stage",
        "div.g-recaptcha",
        "iframe[src*='turnstile']"
    )

    fun buildProbeScript(
        authSelectors: List<String> = AUTH_SELECTORS,
        loginSelectors: List<String> = LOGIN_SELECTORS,
        challengeSelectors: List<String> = CHALLENGE_SELECTORS
    ): String {
        val authJson = authSelectors.joinToString("\", \"", "[\"", "\"]")
        val loginJson = loginSelectors.joinToString("\", \"", "[\"", "\"]")
        val challengeJson = challengeSelectors.joinToString("\", \"", "[\"", "\"]")
        return """
            (function() {
                function isVisible(el) {
                    if (!el) return false;
                    const style = window.getComputedStyle(el);
                    if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return false;
                    const rect = el.getBoundingClientRect();
                    return rect.width > 0 && rect.height > 0;
                }

                function hasVisible(selectors) {
                    for (let i = 0; i < selectors.length; i++) {
                        const elements = document.querySelectorAll(selectors[i]);
                        for (let j = 0; j < elements.length; j++) {
                            if (isVisible(elements[j])) return true;
                        }
                    }
                    return false;
                }

                const challengeSelectors = $challengeJson;
                if (hasVisible(challengeSelectors)) return 'CHALLENGE';

                const authSelectors = $authJson;
                if (hasVisible(authSelectors)) return 'LOGGED_IN';

                const loginSelectors = $loginJson;
                if (hasVisible(loginSelectors)) return 'LOGIN_REQUIRED';

                // ChatGPT mobile keeps its strong account marker inside the collapsed sidebar.
                // Reveal that existing UI once; the next bounded poll can then observe the
                // provider-owned accounts-profile-button without reading account content.
                const sidebar = document.querySelector("button[data-testid='open-sidebar-button']");
                if (isVisible(sidebar)) sidebar.click();
                return 'UNKNOWN';
            })()
        """.trimIndent()
    }

    fun classify(
        hasAuthenticatedMarker: Boolean,
        hasLoginMarker: Boolean,
        hasChallengeMarker: Boolean = false
    ): LoginStatus {
        return when {
            hasAuthenticatedMarker -> LoginStatus.LOGGED_IN
            hasChallengeMarker -> LoginStatus.UNKNOWN
            hasLoginMarker -> LoginStatus.LOGIN_REQUIRED
            else -> LoginStatus.UNKNOWN
        }
    }

    fun parseProbeResult(raw: String?): LoginStatus {
        val clean = raw?.replace("\"", "")?.trim()?.uppercase() ?: return LoginStatus.UNKNOWN
        return when (clean) {
            "LOGGED_IN" -> LoginStatus.LOGGED_IN
            "LOGIN_REQUIRED" -> LoginStatus.LOGIN_REQUIRED
            else -> LoginStatus.UNKNOWN
        }
    }

    fun shouldBlockScanStart(
        loginStatus: LoginStatus,
        explicitLogout: Boolean
    ): Boolean {
        if (explicitLogout) return true
        return loginStatus == LoginStatus.LOGIN_REQUIRED
    }

    fun canInspectUrl(url: String?): Boolean {
        val candidate = url ?: return false
        val host = runCatching { URI(candidate).host?.lowercase() }.getOrNull() ?: return false
        return host == "chatgpt.com" || host.endsWith(".chatgpt.com")
    }
}

class AIBILoginStatusStore(
    private val context: Context,
    private val registry: AIBIProviderRegistry
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _status = MutableStateFlow(LoginStatus.CHECKING)
    val status: StateFlow<LoginStatus> = _status.asStateFlow()

    private var probeWebView: WebView? = null
    private var probeJob: Job? = null
    private var probeContainerRef: WeakReference<ViewGroup>? = null

    fun checkStatus(container: ViewGroup? = null) {
        if (container != null) probeContainerRef = WeakReference(container)
        _status.value = LoginStatus.CHECKING

        probeJob?.cancel()
        probeJob = null
        destroyProbeWebView()

        Handler(Looper.getMainLooper()).post {
            val attachedContainer = container ?: probeContainerRef?.get()
            if (attachedContainer?.isAttachedToWindow == true) {
                probeViaWebView(attachedContainer)
            } else {
                _status.value = LoginStatus.UNKNOWN
            }
        }
    }

    private fun probeViaWebView(targetParent: ViewGroup) {
        val hostContext = targetParent.context
        val density = hostContext.resources.displayMetrics.density
        val widthPx = (375 * density).toInt()
        val heightPx = (667 * density).toInt()

        val webView = WebView(hostContext).apply {
            layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
            alpha = 0.001f
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            isClickable = false
            isFocusable = false
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        cookieManager.setAcceptThirdPartyCookies(webView, true)

        targetParent.addView(webView)
        probeWebView = webView

        val probeScript = AIBILoginClassifier.buildProbeScript()

        var isDone = false
        fun finishWith(status: LoginStatus) {
            if (isDone) return
            isDone = true
            _status.value = status
            probeJob?.cancel()
            probeJob = null
            destroyProbeWebView()
        }

        fun evaluateProbe() {
            if (isDone) return
            if (!AIBILoginClassifier.canInspectUrl(webView.url)) return
            webView.evaluateJavascript(probeScript) { result ->
                val parsed = AIBILoginClassifier.parseProbeResult(result)
                if (parsed == LoginStatus.LOGGED_IN || parsed == LoginStatus.LOGIN_REQUIRED) {
                    finishWith(parsed)
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (AIBILoginClassifier.canInspectUrl(url)) evaluateProbe()
            }
        }

        val targetUrl = try {
            registry.getProviderConfig("chatgpt").initialUrl
        } catch (_: Exception) {
            "https://chatgpt.com/"
        }

        webView.loadUrl(targetUrl)

        // Bounded SPA hydration polling and finite timeout (10s)
        probeJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val timeoutMs = 10_000L
            while (isActive && !isDone && System.currentTimeMillis() - startTime < timeoutMs) {
                delay(500L)
                evaluateProbe()
            }
            if (!isDone) {
                // Finite timeout reached with neither positive authenticated nor login indicator -> UNKNOWN
                finishWith(LoginStatus.UNKNOWN)
            }
        }
    }

    private fun destroyProbeWebView() {
        probeWebView?.apply {
            stopLoading()
            webViewClient = WebViewClient()
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        probeWebView = null
    }

    fun setStatus(status: LoginStatus) {
        probeJob?.cancel()
        probeJob = null
        destroyProbeWebView()
        _status.value = status
    }

    fun close() {
        probeJob?.cancel()
        probeJob = null
        scope.cancel()
        destroyProbeWebView()
        probeContainerRef = null
    }
}
