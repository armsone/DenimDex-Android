package com.armsone.denimdex.core.aibi

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.util.UUID

class AIBISession(
    private val context: Context,
    private val runtimeJavaScript: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    val timingProfile: AIBITimingProfile = AIBITimingProfile.default
) {
    private val _currentPhase = MutableStateFlow(AIBIPhase.IDLE)
    val currentPhase: StateFlow<AIBIPhase> = _currentPhase.asStateFlow()

    private val _progress = MutableStateFlow(AIBIProgress.initial)
    val progress: StateFlow<AIBIProgress> = _progress.asStateFlow()

    private val _isVisibleBrowserPresented = MutableStateFlow(false)
    val isVisibleBrowserPresented: StateFlow<Boolean> = _isVisibleBrowserPresented.asStateFlow()

    private val _activeProviderId = MutableStateFlow<String?>(null)
    val activeProviderId: StateFlow<String?> = _activeProviderId.asStateFlow()

    private val _pendingResult = MutableStateFlow<AIBIResult?>(null)
    val pendingResult: StateFlow<AIBIResult?> = _pendingResult.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    var resultSink: AIBIResultSink? = null

    private var activeTask: AIBITask? = null
    private var activeConfig: AIBIProviderConfig? = null
    private var generationId: Long = 0L

    private var hiddenWebView: WebView? = null
    var visibleWebView: WebView? = null
        private set

    private var activeJob: Job? = null
    private var elapsedJob: Job? = null
    private var nativeAttachmentDirectory: File? = null
    private var nativeAttachmentUris: List<Uri> = emptyList()
    private var nativeAttachmentNextSingleIndex = 0
    private var taskStartTimeMs: Long = 0L
    private var consecutiveMisses = 0
    private var submitAttemptCount = 0
    private var baselineAssistantCount = 0
    private var baselineUserCount = 0
    private var baselineUrl = ""
    private var stabilityText: String? = null
    private var stabilityTickCount = 0

    init {
        configureCookieManager()
    }

    private fun configureCookieManager() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
    }

    fun startTask(task: AIBITask, providerConfig: AIBIProviderConfig, parentViewGroup: ViewGroup? = null) {
        cancelCurrentTask()

        activeTask = task
        activeConfig = providerConfig
        _activeProviderId.value = task.providerId
        generationId++
        taskStartTimeMs = System.currentTimeMillis()
        _lastErrorMessage.value = null
        _pendingResult.value = null

        if (task.attachments.size > 20 ||
            (task.attachments.isNotEmpty() &&
                (!providerConfig.mediaCapabilities.supportsImages ||
                    task.attachments.size > providerConfig.mediaCapabilities.maxImagesPerTask))) {
            failWithError("Image attachments are not supported for this task.")
            return
        }

        updatePhase(AIBIPhase.INITIALIZING, "Connecting to ${providerConfig.displayName}...")
        startElapsedTimer()

        val currentGen = generationId
        scope.launch {
            val prepared = try {
                withContext(Dispatchers.IO) {
                    prepareNativeAttachmentBatch(task.attachments)
                }
            } catch (_: Exception) {
                if (generationId == currentGen) {
                    failWithError("Could not prepare image attachments.")
                }
                return@launch
            }
            if (generationId != currentGen) {
                prepared.first?.deleteRecursively()
                return@launch
            }
            nativeAttachmentDirectory = prepared.first
            nativeAttachmentUris = prepared.second
            nativeAttachmentNextSingleIndex = 0

            if (task.presentation == AIBIPresentationPreference.ALWAYS_VISIBLE) {
                presentVisibleBrowser()
            } else if (parentViewGroup != null) {
                mountHiddenBrowser(parentViewGroup)
            } else {
                presentVisibleBrowser()
            }

            val webView = activeWebView
            updatePhase(AIBIPhase.NAVIGATING, "Loading ${providerConfig.displayName}...")
            webView?.loadUrl(providerConfig.initialUrl)
            scheduleReadinessCheck(currentGen)
        }
    }

    fun manualCopyPrompt() {
        val prompt = activeTask?.promptText ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("AIBI Prompt", prompt)
        clipboard?.setPrimaryClip(clip)
    }

    fun manualImportText(text: String) {
        val task = activeTask ?: return
        val cleaned = cleanOutputLocally(text)
        val result = AIBIResult(
            taskId = task.id,
            providerId = task.providerId,
            rawText = text,
            cleanedText = cleaned,
            isComplete = true
        )
        completeWithResult(result)
    }

    fun cancelCurrentTask() {
        stopAllJobs()
        generationId++
        if (_currentPhase.value != AIBIPhase.IDLE) {
            updatePhase(AIBIPhase.CANCELLED, "Cancelled")
        }
        destroyHiddenBrowser()
        disposeNativeAttachmentBatch()
    }

    fun fullReset() {
        stopAllJobs()
        generationId++
        activeTask = null
        activeConfig = null
        _activeProviderId.value = null
        _pendingResult.value = null
        _lastErrorMessage.value = null
        destroyHiddenBrowser()
        destroyVisibleBrowser()
        disposeNativeAttachmentBatch()
        updatePhase(AIBIPhase.IDLE, "Ready")
    }

    private fun updatePhase(phase: AIBIPhase, message: String, isWaiting: Boolean = false) {
        _currentPhase.value = phase
        val elapsed = if (taskStartTimeMs > 0) (System.currentTimeMillis() - taskStartTimeMs) / 1000.0 else 0.0
        _progress.value = AIBIProgress(
            phase = phase,
            elapsedSeconds = elapsed,
            statusMessage = message,
            isWaiting = isWaiting
        )
    }

    private fun startElapsedTimer() {
        elapsedJob?.cancel()
        elapsedJob = scope.launch {
            while (isActive && taskStartTimeMs > 0) {
                delay(500L)
                val elapsed = (System.currentTimeMillis() - taskStartTimeMs) / 1000.0
                _progress.value = AIBIProgress(
                    phase = _currentPhase.value,
                    elapsedSeconds = elapsed,
                    statusMessage = _progress.value.statusMessage,
                    isWaiting = _progress.value.isWaiting
                )
            }
        }
    }

    private fun stopAllJobs() {
        activeJob?.cancel()
        activeJob = null
        elapsedJob?.cancel()
        elapsedJob = null
    }

    private fun scheduleReadinessCheck(generation: Long) {
        activeJob?.cancel()
        consecutiveMisses = 0
        val startTime = System.currentTimeMillis()

        activeJob = scope.launch {
            while (isActive && generationId == generation) {
                if (System.currentTimeMillis() - startTime > timingProfile.readinessTimeoutMs) {
                    escalateToVisible(AIBIFallbackReason.READINESS_TIMEOUT)
                    break
                }

                performReadinessProbe(generation)
                delay(timingProfile.readinessCadenceMs)
            }
        }
    }

    private suspend fun performReadinessProbe(generation: Long) {
        val config = activeConfig ?: return
        val webView = activeWebView ?: return
        ensureRuntimeInjected(webView)

        val configJsonStr = buildConfigJsonString(config)
        val script = "window.__AIBI_RUNTIME__.checkReadiness($configJsonStr)"
        val rawResult = evaluateScript(webView, script) ?: return
        if (generationId != generation) return

        try {
            val json = JSONObject(rawResult)
            if (!json.optBoolean("success", false)) return
            val data = json.optJSONObject("data") ?: return

            val isReady = data.optBoolean("isReady", false)
            val isLoggedIn = data.optBoolean("isLoggedIn", true)
            val hasChallenge = data.optBoolean("hasChallenge", false)
            val reason = data.optString("reason")

            if (!isLoggedIn) {
                activeJob?.cancel()
                escalateToVisible(AIBIFallbackReason.AUTH_REQUIRED)
                return
            }

            if (hasChallenge) {
                activeJob?.cancel()
                escalateToVisible(AIBIFallbackReason.SECURITY_CHALLENGE_PRESENTED)
                return
            }

            if (isReady) {
                activeJob?.cancel()
                recordBaselineAndInject(generation)
            } else if (reason == "INPUT_NOT_FOUND") {
                consecutiveMisses++
                if (consecutiveMisses >= timingProfile.maxReadinessMisses) {
                    activeJob?.cancel()
                    escalateToVisible(AIBIFallbackReason.INPUT_NOT_FOUND)
                }
            }
        } catch (_: Exception) {
            // Continue until deadline
        }
    }

    private fun recordBaselineAndInject(generation: Long) {
        val config = activeConfig ?: return
        val webView = activeWebView ?: return
        val task = activeTask ?: return
        updatePhase(AIBIPhase.INJECTING_PROMPT, "Preparing prompt...")

        scope.launch {
            val configJsonStr = buildConfigJsonString(config)

            // Baseline
            val baselineScript = "window.__AIBI_RUNTIME__.getBaselineState($configJsonStr)"
            val baselineResult = evaluateScript(webView, baselineScript)
            if (baselineResult != null) {
                try {
                    val json = JSONObject(baselineResult)
                    val data = json.optJSONObject("data")
                    baselineAssistantCount = data?.optInt("assistantCount", 0) ?: 0
                    baselineUserCount = data?.optInt("userCount", 0) ?: 0
                    baselineUrl = data?.optString("currentUrl", "") ?: ""
                } catch (_: Exception) {}
            }

            if (task.attachments.isNotEmpty() && !attachImagesAtomically(webView, config, task, generation)) {
                if (generationId == generation) escalateToVisible(AIBIFallbackReason.ATTACHMENT_FAILED)
                return@launch
            }

            if (generationId != generation) return@launch

            // Bounded prompt injection & persistence verification loop
            val escapedPrompt = JSONObject.quote(task.promptText)
            val maxAttempts = timingProfile.promptInjectionMaxAttempts
            val retryDelayMs = timingProfile.promptInjectionRetryDelayMs

            var attempt = 1
            var injectionSucceeded = false

            while (attempt <= maxAttempts && generationId == generation) {
                val injectScript = "window.__AIBI_RUNTIME__.injectPrompt($configJsonStr, $escapedPrompt, ${task.forceFill})"
                val rawInjectResult = evaluateScript(webView, injectScript)
                if (generationId != generation) return@launch

                val injectResult = AIBIPromptInjectionClassifier.parseInjectionResult(rawInjectResult)

                if (injectResult.errorCode == "EXISTING_TEXT_PRESERVED") {
                    // Terminal: user text must never be overwritten or retried.
                    failWithError("ChatGPT 입력창에 이미 다른 내용이 있어 자동 입력을 건너뛰었습니다. 직접 확인해주세요.")
                    return@launch
                }

                val verifyResult = if (injectResult.isSuccess) {
                    val verifyScript = "window.__AIBI_RUNTIME__.verifyPromptInjected($configJsonStr, $escapedPrompt)"
                    val rawVerifyResult = evaluateScript(webView, verifyScript)
                    if (generationId != generation) return@launch
                    AIBIPromptInjectionClassifier.parseVerifyResult(rawVerifyResult)
                } else {
                    null
                }

                val outcome = AIBIPromptInjectionClassifier.classify(injectResult, verifyResult)
                if (outcome == AIBIPromptInjectionOutcome.SUCCESS_VERIFIED) {
                    injectionSucceeded = true
                    break
                }

                if (attempt < maxAttempts) {
                    delay(retryDelayMs)
                    if (generationId != generation) return@launch
                }
                attempt++
            }

            if (generationId != generation) return@launch

            if (injectionSucceeded) {
                startSubmissionLoop(generation)
            } else {
                escalateToVisible(AIBIFallbackReason.INPUT_NOT_FOUND)
            }
        }
    }

    private suspend fun attachImagesAtomically(
        webView: WebView,
        config: AIBIProviderConfig,
        task: AIBITask,
        generation: Long
    ): Boolean {
        updatePhase(AIBIPhase.ATTACHING_MEDIA, "Attaching ${task.attachments.size} photos...", isWaiting = true)
        val configJsonStr = buildConfigJsonString(config)
        val stateScript = "window.__AIBI_RUNTIME__.getAttachmentState($configJsonStr)"
        val baseline = parseAttachmentPreviewCount(evaluateScript(webView, stateScript)) ?: 0
        val expectedTotal = baseline + task.attachments.size
        nativeAttachmentNextSingleIndex = 0

        val prepareScript = "window.__AIBI_RUNTIME__.prepareAttachmentInput($configJsonStr)"
        evaluateScript(webView, prepareScript)
        delay(700L)
        if (generationId != generation) return false

        var observedCount = baseline
        val nativeOverallDeadline = System.currentTimeMillis() + timingProfile.attachmentTimeoutMs
        while (generationId == generation && observedCount < expectedTotal &&
            System.currentTimeMillis() < nativeOverallDeadline) {
            val panelResult = evaluateScript(
                webView,
                "window.__AIBI_RUNTIME__.openAttachmentPanel($configJsonStr)"
            )
            if (panelResult == null || !parseRuntimeSuccess(panelResult)) {
                delay(timingProfile.attachmentCadenceMs)
                continue
            }
            val previousCount = observedCount
            val nativePreviewWaitMs = 6_000L
            val nativeStepDeadline = minOf(
                nativeOverallDeadline,
                System.currentTimeMillis() + nativePreviewWaitMs
            )
            while (generationId == generation && System.currentTimeMillis() < nativeStepDeadline) {
                observedCount = parseAttachmentPreviewCount(evaluateScript(webView, stateScript)) ?: 0
                if (observedCount == expectedTotal) return true
                if (observedCount > previousCount) break
                delay(timingProfile.attachmentCadenceMs)
            }
            if (observedCount <= previousCount) break
        }

        // Bounded DataTransfer fallback
        val ordered = task.attachments.sortedBy { it.sourceIndex }
        val beginResult = evaluateScript(
            webView,
            "window.__AIBI_RUNTIME__.beginAttachmentBatch($configJsonStr, ${ordered.size})"
        ) ?: return false
        if (!parseRuntimeSuccess(beginResult)) return false

        ordered.forEachIndexed { index, attachment ->
            val imageJson = JSONObject(mapOf(
                "dataUrl" to attachment.dataUrl(),
                "mimeType" to attachment.mimeType,
                "filename" to attachment.filename
            ))
            val staged = evaluateScript(
                webView,
                "window.__AIBI_RUNTIME__.stageAttachment($imageJson, $index)"
            )
            if (staged == null || !parseRuntimeSuccess(staged)) {
                evaluateScript(webView, "window.__AIBI_RUNTIME__.clearAttachmentBatch()")
                return false
            }
        }
        val attachResult = evaluateScript(
            webView,
            "window.__AIBI_RUNTIME__.commitAttachmentBatch($configJsonStr)"
        ) ?: return false
        if (!parseRuntimeSuccess(attachResult)) return false

        val deadline = System.currentTimeMillis() + timingProfile.attachmentTimeoutMs
        while (generationId == generation && System.currentTimeMillis() < deadline) {
            val previewCount = parseAttachmentPreviewCount(evaluateScript(webView, stateScript)) ?: 0
            if (previewCount == baseline + task.attachments.size) return true
            delay(timingProfile.attachmentCadenceMs)
        }
        return false
    }

    private fun parseAttachmentPreviewCount(raw: String?): Int? = try {
        JSONObject(raw ?: return null).optJSONObject("data")?.optInt("previewCount")
    } catch (_: Exception) {
        null
    }

    private fun parseRuntimeSuccess(raw: String): Boolean = try {
        JSONObject(raw).optBoolean("success", false)
    } catch (_: Exception) {
        false
    }

    private fun startSubmissionLoop(generation: Long) {
        activeJob?.cancel()
        submitAttemptCount = 1
        val startTime = System.currentTimeMillis()
        updatePhase(AIBIPhase.SUBMITTING, "Sending prompt...")

        activeJob = scope.launch {
            while (isActive && generationId == generation) {
                if (System.currentTimeMillis() - startTime > timingProfile.submitTimeoutMs) {
                    escalateToVisible(AIBIFallbackReason.INPUT_NOT_FOUND)
                    break
                }

                performSubmitAttempt(generation)
                delay(timingProfile.submitCadenceMs + timingProfile.submitVerificationDelayMs)
            }
        }
    }

    private suspend fun performSubmitAttempt(generation: Long) {
        val config = activeConfig ?: return
        val webView = activeWebView ?: return
        val configJsonStr = buildConfigJsonString(config)

        val submitScript = "window.__AIBI_RUNTIME__.submitPrompt($configJsonStr, $submitAttemptCount)"
        evaluateScript(webView, submitScript)
        if (generationId != generation) return

        delay(timingProfile.submitVerificationDelayMs)
        if (generationId != generation) return

        val verifyScript = "window.__AIBI_RUNTIME__.verifySubmission(" +
            "$configJsonStr, $baselineAssistantCount, $baselineUserCount, ${JSONObject.quote(baselineUrl)})"
        val verifyResult = evaluateScript(webView, verifyScript)
        if (verifyResult != null) {
            try {
                val json = JSONObject(verifyResult)
                val data = json.optJSONObject("data")
                if (data?.optBoolean("submitted", false) == true) {
                    activeJob?.cancel()
                    startObservationLoop(generation)
                    return
                }
            } catch (_: Exception) {}
        }

        submitAttemptCount++
    }

    private fun startObservationLoop(generation: Long) {
        activeJob?.cancel()
        stabilityText = null
        stabilityTickCount = 0
        updatePhase(AIBIPhase.GENERATING, "Waiting for answer...", isWaiting = true)

        activeJob = scope.launch {
            while (isActive && generationId == generation) {
                performObservationTick(generation)
                delay(timingProfile.observationCadenceMs)
            }
        }
    }

    private suspend fun performObservationTick(generation: Long) {
        val config = activeConfig ?: return
        val webView = activeWebView ?: return
        val task = activeTask ?: return
        val configJsonStr = buildConfigJsonString(config)

        val script = "window.__AIBI_RUNTIME__.observeGeneration($configJsonStr, $baselineAssistantCount)"
        val result = evaluateScript(webView, script) ?: return
        if (generationId != generation) return

        try {
            val json = JSONObject(result)
            if (!json.optBoolean("success", false)) return
            val data = json.optJSONObject("data") ?: return

            val phaseStr = data.optString("phase", "GENERATING")
            val isGenerating = data.optBoolean("isGenerating", true)
            val hasNewAnswer = data.optBoolean("hasNewAnswer", false)
            val rawText = data.optString("rawText", "")
            val errorMessage = data.optString("errorMessage").takeIf { it.isNotEmpty() && it != "null" }

            if (phaseStr == "FAILED" && !errorMessage.isNullOrEmpty()) {
                activeJob?.cancel()
                failWithError(errorMessage)
                return
            }

            if (phaseStr == "FALLBACK_REQUIRED") {
                activeJob?.cancel()
                escalateToVisible(AIBIFallbackReason.SECURITY_CHALLENGE_PRESENTED)
                return
            }

            if (hasNewAnswer && !isGenerating && rawText.trim().isNotEmpty()) {
                if (stabilityText != null && stabilityText == rawText) {
                    stabilityTickCount++
                    // stabilityRequiredTicks = 2 means 3 matching consecutive ticks
                    if (stabilityTickCount >= timingProfile.stabilityRequiredTicks) {
                        // Do not cancel the observation job here: this function is
                        // running inside that job and still needs to suspend while
                        // cleaning and committing the result. completeWithResult()
                        // owns cancellation after the handoff succeeds.
                        val cleanScript = "window.__AIBI_RUNTIME__.cleanOutput(${JSONObject.quote(rawText)}, '${task.providerId}')"
                        var cleaned = rawText
                        val cleanResult = evaluateScript(webView, cleanScript)
                        if (cleanResult != null) {
                            try {
                                val cleanJson = JSONObject(cleanResult)
                                val cleanData = cleanJson.optJSONObject("data")
                                cleaned = cleanData?.optString("cleanedText", rawText) ?: rawText
                            } catch (_: Exception) {}
                        }

                        val finalResult = AIBIResult(
                            taskId = task.id,
                            providerId = task.providerId,
                            rawText = rawText,
                            cleanedText = cleaned,
                            isComplete = true
                        )
                        completeWithResult(finalResult)
                    }
                } else {
                    stabilityText = rawText
                    stabilityTickCount = 0
                    updatePhase(AIBIPhase.STABILIZING, "Receiving answer...", isWaiting = true)
                }
            } else {
                stabilityText = null
                stabilityTickCount = 0
                updatePhase(AIBIPhase.GENERATING, "Waiting for answer...", isWaiting = true)
            }
        } catch (_: Exception) {}
    }

    private fun completeWithResult(result: AIBIResult) {
        stopAllJobs()
        _pendingResult.value = result

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("AIBI Result", result.cleanedText)
        clipboard?.setPrimaryClip(clip)

        val sink = resultSink
        if (sink != null) {
            val commitOutcome = sink.commitResult(result)
            if (commitOutcome.isSuccess) {
                updatePhase(AIBIPhase.COMPLETED, "Import completed")
                dismissVisibleBrowser()
                destroyHiddenBrowser()
                disposeNativeAttachmentBatch()
            } else {
                val err = commitOutcome.exceptionOrNull()?.message ?: "Validation error"
                updatePhase(AIBIPhase.FAILED, "Host import validation failed: $err")
            }
        } else {
            updatePhase(AIBIPhase.COMPLETED, "Result ready")
            dismissVisibleBrowser()
            destroyHiddenBrowser()
            disposeNativeAttachmentBatch()
        }
    }

    private fun failWithError(message: String) {
        stopAllJobs()
        _lastErrorMessage.value = message
        updatePhase(AIBIPhase.FAILED, message)
        destroyHiddenBrowser()
        disposeNativeAttachmentBatch()
    }

    private fun escalateToVisible(reason: AIBIFallbackReason) {
        stopAllJobs()
        updatePhase(AIBIPhase.FALLBACK_REQUIRED, "Opening browser for required action...")

        destroyHiddenBrowser()
        presentVisibleBrowser()

        val config = activeConfig ?: return
        visibleWebView?.loadUrl(config.initialUrl)
    }

    private val activeWebView: WebView?
        get() = if (_isVisibleBrowserPresented.value) visibleWebView else hiddenWebView

    @SuppressLint("SetJavaScriptEnabled")
    private fun mountHiddenBrowser(parent: ViewGroup) {
        if (hiddenWebView != null) return

        val density = context.resources.displayMetrics.density
        val widthPx = (375 * density).toInt()
        val heightPx = (667 * density).toInt()

        val webView = WebView(context).apply {
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
            webViewClient = createSecurityWebViewClient()
            webChromeClient = createAttachmentWebChromeClient()
        }

        hiddenWebView = webView
        parent.addView(webView)
    }

    private fun destroyHiddenBrowser() {
        hiddenWebView?.apply {
            stopLoading()
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        hiddenWebView = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun presentVisibleBrowser() {
        if (visibleWebView == null) {
            val promoted = hiddenWebView
            if (promoted != null) {
                (promoted.parent as? ViewGroup)?.removeView(promoted)
                promoted.alpha = 1f
                promoted.isClickable = true
                promoted.isFocusable = true
                promoted.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                visibleWebView = promoted
                hiddenWebView = null
            } else {
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportMultipleWindows(true)
                    webViewClient = createSecurityWebViewClient()
                    webChromeClient = createAttachmentWebChromeClient(this)
                }
                visibleWebView = webView
            }
        }
        _isVisibleBrowserPresented.value = true
    }

    fun dismissVisibleBrowser() {
        _isVisibleBrowserPresented.value = false
    }

    fun destroyVisibleBrowser() {
        visibleWebView?.apply {
            stopLoading()
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            destroy()
        }
        visibleWebView = null
        _isVisibleBrowserPresented.value = false
    }

    private fun createAttachmentWebChromeClient(
        popupTarget: WebView? = null
    ): WebChromeClient = object : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            val callback = filePathCallback ?: return false
            if (nativeAttachmentUris.isEmpty()) return false
            if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                nativeAttachmentNextSingleIndex = nativeAttachmentUris.size
                callback.onReceiveValue(nativeAttachmentUris.toTypedArray())
            } else {
                val next = nativeAttachmentUris.getOrNull(nativeAttachmentNextSingleIndex)
                if (next == null) {
                    callback.onReceiveValue(null)
                } else {
                    nativeAttachmentNextSingleIndex += 1
                    callback.onReceiveValue(arrayOf(next))
                }
            }
            return true
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message?
        ): Boolean {
            val target = popupTarget ?: return false
            if (!isUserGesture) return false
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            val popup = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        popupView: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url ?: return true
                        if (url.toString() == "about:blank") return false
                        val config = activeConfig ?: return true
                        val allowed = originAllowed(url, config.allowedScriptOrigins) ||
                            originAllowed(url, config.allowedAuthOrigins)
                        if (allowed) {
                            target.loadUrl(url.toString())
                        } else {
                            failWithError("This sign-in page is not in the allowed origin list.")
                        }
                        popupView?.stopLoading()
                        popupView?.destroy()
                        return true
                    }
                }
            }
            transport.webView = popup
            resultMsg.sendToTarget()
            return true
        }
    }

    private fun prepareNativeAttachmentBatch(
        attachments: List<AIBIMediaAttachment>
    ): Pair<File?, List<Uri>> {
        if (attachments.isEmpty()) return null to emptyList()
        val root = File(context.cacheDir, "aibi").apply { mkdirs() }
        root.listFiles()?.filter(File::isDirectory)?.forEach { stale ->
            if (System.currentTimeMillis() - stale.lastModified() > 15 * 60 * 1_000L) {
                stale.deleteRecursively()
            }
        }
        val directory = File(root, "batch-${UUID.randomUUID()}").apply { mkdirs() }
        return try {
            val uris = attachments.sortedBy { it.sourceIndex }.mapIndexed { index, attachment ->
                val file = File(directory, "aibi-${(index + 1).toString().padStart(2, '0')}.jpg")
                file.writeBytes(attachment.data)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            directory to uris
        } catch (error: Exception) {
            directory.deleteRecursively()
            throw error
        }
    }

    private fun disposeNativeAttachmentBatch() {
        nativeAttachmentDirectory?.deleteRecursively()
        nativeAttachmentDirectory = null
        nativeAttachmentUris = emptyList()
        nativeAttachmentNextSingleIndex = 0
    }

    private fun createSecurityWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                val config = activeConfig ?: return false

                val isScriptOrigin = originAllowed(url, config.allowedScriptOrigins)
                val isAuthOrigin = originAllowed(url, config.allowedAuthOrigins)

                return if (isScriptOrigin || isAuthOrigin) {
                    false
                } else {
                    if (_isVisibleBrowserPresented.value) {
                        failWithError("This sign-in page is not in the allowed origin list.")
                    } else {
                        escalateToVisible(AIBIFallbackReason.NAVIGATION_DISALLOWED)
                    }
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val config = activeConfig ?: return
                val pageUri = url?.let(Uri::parse) ?: return
                if (originAllowed(pageUri, config.allowedScriptOrigins)) view?.let {
                    scope.launch { ensureRuntimeInjected(it) }
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val sanitized = "Network error: ${error?.description ?: "Connection failed"}"
                    failWithError(sanitized)
                }
            }
        }
    }

    private fun originAllowed(url: Uri, origins: List<String>): Boolean {
        return origins.any { candidate ->
            val allowed = Uri.parse(candidate)
            url.scheme.equals(allowed.scheme, ignoreCase = true) &&
                url.host.equals(allowed.host, ignoreCase = true) &&
                url.port == allowed.port
        }
    }

    private suspend fun ensureRuntimeInjected(webView: WebView) {
        val config = activeConfig ?: return
        val pageUri = webView.url?.let(Uri::parse) ?: return
        if (runtimeJavaScript.isEmpty() || !originAllowed(pageUri, config.allowedScriptOrigins)) return
        val checkScript = "typeof window.__AIBI_RUNTIME__ !== 'undefined'"
        val exists = evaluateScript(webView, checkScript)
        if (exists == "true") return
        evaluateScript(webView, runtimeJavaScript)
    }

    private suspend fun evaluateScript(webView: WebView, script: String): String? {
        return suspendCancellableCoroutine { continuation ->
            Handler(Looper.getMainLooper()).post {
                webView.evaluateJavascript(script) { result ->
                    val sanitized = if (result != null && result != "null") {
                        if (result.startsWith("\"") && result.endsWith("\"") && result.length >= 2) {
                            try {
                                JSONObject("{v:$result}").getString("v")
                            } catch (_: Exception) {
                                result.substring(1, result.length - 1)
                            }
                        } else {
                            result
                        }
                    } else {
                        null
                    }
                    continuation.resume(sanitized) {}
                }
            }
        }
    }

    private fun buildConfigJsonString(config: AIBIProviderConfig): String {
        return JSONObject(mapOf(
            "selectors" to JSONObject(mapOf(
                "promptInput" to config.selectors.promptInput,
                "submitButton" to config.selectors.submitButton,
                "stopButton" to config.selectors.stopButton,
                "assistantMessage" to config.selectors.assistantMessage,
                "preCode" to (config.selectors.preCode ?: listOf("pre code")),
                "errorBanner" to config.selectors.errorBanner,
                "loginIndicator" to config.selectors.loginIndicator,
                "challengeIndicator" to config.selectors.challengeIndicator,
                "attachmentInput" to config.selectors.attachmentInput,
                "attachmentTrigger" to config.selectors.attachmentTrigger,
                "attachmentMenuAction" to config.selectors.attachmentMenuAction,
                "attachmentMenuActionText" to config.selectors.attachmentMenuActionText,
                "attachmentPreview" to config.selectors.attachmentPreview
            )),
            "mediaCapabilities" to JSONObject(mapOf(
                "supportsImages" to config.mediaCapabilities.supportsImages,
                "maxImagesPerTask" to config.mediaCapabilities.maxImagesPerTask,
                "requiresMultipleInputForBatch" to config.mediaCapabilities.requiresMultipleInputForBatch
            ))
        )).toString()
    }

    private fun cleanOutputLocally(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```") && text.endsWith("```")) {
            val lines = text.lines()
            if (lines.size >= 2) {
                text = lines.subList(1, lines.size - 1).joinToString("\n").trim()
            }
        }
        return text
    }
}
