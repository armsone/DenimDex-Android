package com.armsone.denimdex.core.aibi

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.graphics.Color
import com.armsone.denimdex.core.design.DenimColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class LoginStatus(val title: String, val iconName: String, val color: Color) {
    CHECKING("확인 중", "arrow.triangle.2.circlepath", DenimColors.inkSoft),
    LOGGED_IN("로그인됨", "checkmark.circle.fill", DenimColors.successGreen),
    LOGIN_REQUIRED("로그인 필요", "exclamationmark.circle.fill", DenimColors.warningAmber);
}

class AIBILoginStatusStore(
    private val context: Context,
    private val registry: AIBIProviderRegistry
) {
    private val _status = MutableStateFlow(LoginStatus.CHECKING)
    val status: StateFlow<LoginStatus> = _status.asStateFlow()

    private var probeWebView: WebView? = null

    init {
        checkStatus()
    }

    fun checkStatus() {
        _status.value = LoginStatus.CHECKING

        val cookieManager = CookieManager.getInstance()
        val chatgptCookies = cookieManager.getCookie("https://chatgpt.com") ?: ""
        val openaiCookies = cookieManager.getCookie("https://chat.openai.com") ?: ""

        val hasSessionCookie = chatgptCookies.contains("__Secure-") ||
            chatgptCookies.contains("session") ||
            openaiCookies.contains("__Secure-") ||
            openaiCookies.contains("session") ||
            chatgptCookies.contains("cf_clearance")

        if (!hasSessionCookie) {
            _status.value = LoginStatus.LOGIN_REQUIRED
            return
        }

        // Perform lightweight DOM probe
        Handler(Looper.getMainLooper()).post {
            probeViaWebView()
        }
    }

    private fun probeViaWebView() {
        probeWebView?.destroy()
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(
                        """
                        (function() {
                            const loginBtn = document.querySelector("button[data-testid='login-button'], a[href*='/auth/login']");
                            const userMenu = document.querySelector("button[data-testid='profile-button'], button[data-testid='user-menu-button'], button[data-testid='user-menu']");
                            if (userMenu) return 'logged_in';
                            if (loginBtn) return 'login_required';
                            return 'logged_in';
                        })()
                        """.trimIndent()
                    ) { result ->
                        val clean = result?.replace("\"", "")?.trim()
                        if (clean == "login_required") {
                            _status.value = LoginStatus.LOGIN_REQUIRED
                        } else {
                            _status.value = LoginStatus.LOGGED_IN
                        }
                    }
                }
            }
        }
        probeWebView = webView
        webView.loadUrl("https://chatgpt.com/")
    }

    fun setStatus(status: LoginStatus) {
        _status.value = status
    }
}
