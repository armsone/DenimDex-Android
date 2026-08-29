package com.armsone.denimdex.feature.scan

import android.content.Context
import android.view.ViewGroup
import com.armsone.denimdex.core.aibi.*
import com.armsone.denimdex.core.domain.*
import com.armsone.denimdex.core.model.QuickValuePhotoRoles
import com.armsone.denimdex.core.model.QuickValueResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class QuickValueRunState {
    object Idle : QuickValueRunState()
    data class Preparing(val message: String) : QuickValueRunState()
    data class Running(
        val phase: AIBIPhase,
        val elapsedSeconds: Double? = null,
        val statusMessage: String,
        val sentPhotoCount: Int,
        val excludedSimilarCount: Int = 0,
        val excludedLimitCount: Int = 0
    ) : QuickValueRunState()
    data class Success(
        val result: QuickValueResult,
        val sentPhotoCount: Int,
        val excludedSimilarCount: Int = 0,
        val excludedLimitCount: Int = 0
    ) : QuickValueRunState()
    data class Error(val message: String) : QuickValueRunState()
    object Timeout : QuickValueRunState()
}

sealed class CountdownStatus {
    object NotStarted : CountdownStatus()
    data class Active(val elapsedSeconds: Double) : CountdownStatus()
    data class Expired(val elapsedSeconds: Double) : CountdownStatus()
}

class QuickValueCountdownTracker(
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    var countdownStartTimeMs: Long? = null
        private set

    val isStarted: Boolean
        get() = countdownStartTimeMs != null

    fun onPhaseProgress(phase: AIBIPhase, nowMs: Long = clock()): CountdownStatus {
        if (countdownStartTimeMs == null && isCountdownEligiblePhase(phase)) {
            countdownStartTimeMs = nowMs
        }

        val start = countdownStartTimeMs
        return if (start != null) {
            val elapsed = (nowMs - start) / 1000.0
            if (CountdownFormatter.isExpired(elapsed)) {
                CountdownStatus.Expired(elapsed)
            } else {
                CountdownStatus.Active(elapsed)
            }
        } else {
            CountdownStatus.NotStarted
        }
    }

    fun computeCurrentElapsed(nowMs: Long = clock()): Double? {
        val start = countdownStartTimeMs ?: return null
        return (nowMs - start) / 1000.0
    }

    fun isExpired(nowMs: Long = clock()): Boolean {
        val elapsed = computeCurrentElapsed(nowMs) ?: return false
        return CountdownFormatter.isExpired(elapsed)
    }

    fun reset() {
        countdownStartTimeMs = null
    }

    companion object {
        fun isCountdownEligiblePhase(phase: AIBIPhase): Boolean {
            return phase == AIBIPhase.GENERATING || phase == AIBIPhase.STABILIZING
        }
    }
}

class QuickValueRunner(
    private val context: Context,
    private val registry: AIBIProviderRegistry
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow<QuickValueRunState>(QuickValueRunState.Idle)
    val state: StateFlow<QuickValueRunState> = _state.asStateFlow()

    private var aibiSession: AIBISession? = null
    private var countdownJob: Job? = null
    private var taskJob: Job? = null

    private val countdownTracker = QuickValueCountdownTracker()
    private var activeSentRoles: List<String> = emptyList()

    val countdownStartTimeMs: Long?
        get() = countdownTracker.countdownStartTimeMs

    fun run(
        sourcePhotoBytes: List<ByteArray>,
        hiddenContainer: ViewGroup,
        onResult: (QuickValueResult) -> Unit
    ) {
        cancel()

        if (sourcePhotoBytes.isEmpty()) {
            _state.value = QuickValueRunState.Error("감정할 사진을 최소 1장 이상 추가해주세요.")
            return
        }

        _state.value = QuickValueRunState.Preparing("사진을 선별하고 준비하고 있습니다...")

        taskJob = scope.launch {
            try {
            val totalSourceCount = sourcePhotoBytes.size
            val (selectedPhotos, similarExcluded, limitExcluded) = withContext(Dispatchers.Default) {
                // 1. Deduplicate
                val deduplicated = PhotoDeduplicator.selectBestPhotos(
                    sourcePhotoBytes,
                    targetCount = QuickValueImagePolicy.SEND_MAXIMUM_COUNT
                )
                val similarCount = totalSourceCount - deduplicated.size

                // 2. Limit to max 20
                val bounded = deduplicated.take(QuickValueImagePolicy.SEND_MAXIMUM_COUNT)
                val limitCount = deduplicated.size - bounded.size

                Triple(bounded, similarCount, limitCount)
            }

            val sentCount = selectedPhotos.size
            activeSentRoles = QuickValuePhotoRoles.allRoles(sentCount)

            // 3. Normalize images
            val attachments = withContext(Dispatchers.Default) {
                val policy = QuickValueImagePolicy.policyForCount(sentCount)
                AIBIImageNormalizer.normalizeOrdered(
                    sourceImages = selectedPhotos,
                    roles = activeSentRoles,
                    policy = AIBIImageNormalizationPolicy(
                        maximumImageCount = sentCount,
                        maximumLongEdgePixels = policy.maxLongEdgePixels,
                        maximumBytesPerImage = QuickValueImagePolicy.MAXIMUM_BYTES_PER_IMAGE.toInt(),
                        initialJpegQuality = policy.initialJpegQuality,
                        minimumJpegQuality = policy.minimumJpegQuality
                    )
                )
            }

            // 4. Build prompt
            val promptText = QuickValuePromptBuilder.buildPrompt(sentCount)
            val task = AIBITask(
                id = UUID.randomUUID(),
                providerId = "chatgpt",
                promptText = promptText,
                attachments = attachments,
                presentation = AIBIPresentationPreference.VISIBLE_WHEN_NEEDED,
                forceFill = false
            )

            val config = registry.getProviderConfig("chatgpt")
            val session = AIBISession(
                context = context,
                runtimeJavaScript = registry.runtimeJavaScript,
                scope = scope
            )
            aibiSession = session

            session.resultSink = object : AIBIResultSink {
                override fun commitResult(result: AIBIResult): Result<Unit> {
                    val validated = QuickValueResultValidator.validate(result.cleanedText, activeSentRoles)
                    return if (validated.isSuccess) {
                        val parsed = validated.getOrThrow()
                        countdownJob?.cancel()
                        countdownJob = null
                        countdownTracker.reset()
                        _state.value = QuickValueRunState.Success(
                            result = parsed,
                            sentPhotoCount = sentCount,
                            excludedSimilarCount = similarExcluded,
                            excludedLimitCount = limitExcluded
                        )
                        onResult(parsed)
                        Result.success(Unit)
                    } else {
                        val err = validated.exceptionOrNull()?.message ?: "JSON 스키마 검증 실패"
                        countdownJob?.cancel()
                        countdownJob = null
                        countdownTracker.reset()
                        _state.value = QuickValueRunState.Error("가치 분석 결과를 해석하지 못했습니다: $err")
                        Result.failure(validated.exceptionOrNull() ?: Exception(err))
                    }
                }
            }

            // Listen to session progress
            launch {
                session.progress.collect { prog ->
                    if (_state.value is QuickValueRunState.Running) {
                        val status = countdownTracker.onPhaseProgress(prog.phase)
                        when (status) {
                            is CountdownStatus.Expired -> {
                                countdownJob?.cancel()
                                countdownJob = null
                                countdownTracker.reset()
                                _state.value = QuickValueRunState.Timeout
                                session.cancelCurrentTask()
                            }
                            is CountdownStatus.Active -> {
                                if (countdownJob == null || !countdownJob!!.isActive) {
                                    startHostCountdown()
                                }
                                _state.value = QuickValueRunState.Running(
                                    phase = prog.phase,
                                    elapsedSeconds = status.elapsedSeconds,
                                    statusMessage = prog.statusMessage,
                                    sentPhotoCount = sentCount,
                                    excludedSimilarCount = similarExcluded,
                                    excludedLimitCount = limitExcluded
                                )
                            }
                            is CountdownStatus.NotStarted -> {
                                _state.value = QuickValueRunState.Running(
                                    phase = prog.phase,
                                    elapsedSeconds = null,
                                    statusMessage = prog.statusMessage,
                                    sentPhotoCount = sentCount,
                                    excludedSimilarCount = similarExcluded,
                                    excludedLimitCount = limitExcluded
                                )
                            }
                        }
                    }
                }
            }

            // Listen to session errors
            launch {
                session.lastErrorMessage.collect { err ->
                    if (err != null && _state.value !is QuickValueRunState.Success) {
                        countdownJob?.cancel()
                        countdownJob = null
                        countdownTracker.reset()
                        _state.value = QuickValueRunState.Error(err)
                    }
                }
            }

            _state.value = QuickValueRunState.Running(
                phase = AIBIPhase.INITIALIZING,
                elapsedSeconds = null,
                statusMessage = "ChatGPT에 연결하고 있습니다...",
                sentPhotoCount = sentCount,
                excludedSimilarCount = similarExcluded,
                excludedLimitCount = limitExcluded
            )

            session.startTask(task, config, hiddenContainer)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                countdownJob?.cancel()
                countdownJob = null
                countdownTracker.reset()
                aibiSession?.cancelCurrentTask()
                aibiSession = null
                _state.value = QuickValueRunState.Error(preparationErrorMessage(error))
            }
        }
    }

    private fun preparationErrorMessage(error: Throwable): String = when (error.message) {
        "EMPTY_IMAGE", "UNSUPPORTED_IMAGE", "IMAGE_DECODE_FAILED" ->
            "선택한 사진을 읽지 못했습니다. 다른 사진으로 다시 시도해주세요."
        "IMAGE_ENCODE_FAILED", "IMAGE_SIZE_TARGET_UNREACHABLE" ->
            "사진을 분석용으로 준비하지 못했습니다. 더 작은 원본으로 다시 시도해주세요."
        "ATTACHMENT_LIMIT_EXCEEDED" ->
            "한 번에 보낼 수 있는 사진 수를 초과했습니다."
        else -> "사진 분석을 시작하지 못했습니다. 잠시 후 다시 시도해주세요."
    }

    private fun startHostCountdown() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (isActive) {
                delay(300L)
                val elapsed = countdownTracker.computeCurrentElapsed()
                if (elapsed == null) {
                    break
                }
                if (CountdownFormatter.isExpired(elapsed)) {
                    countdownJob?.cancel()
                    countdownJob = null
                    countdownTracker.reset()
                    if (_state.value is QuickValueRunState.Running) {
                        _state.value = QuickValueRunState.Timeout
                        aibiSession?.cancelCurrentTask()
                    }
                    break
                }
                val current = _state.value
                if (current is QuickValueRunState.Running) {
                    _state.value = current.copy(elapsedSeconds = elapsed)
                }
            }
        }
    }

    fun manualImportResultText(text: String, onResult: (QuickValueResult) -> Unit): Boolean {
        val validated = QuickValueResultValidator.validate(text, activeSentRoles)
        return if (validated.isSuccess) {
            val parsed = validated.getOrThrow()
            countdownJob?.cancel()
            countdownJob = null
            countdownTracker.reset()
            _state.value = QuickValueRunState.Success(
                result = parsed,
                sentPhotoCount = activeSentRoles.size,
                excludedSimilarCount = 0,
                excludedLimitCount = 0
            )
            onResult(parsed)
            true
        } else {
            false
        }
    }

    fun manualCopyPrompt(): String {
        val prompt = QuickValuePromptBuilder.buildPrompt(activeSentRoles.size)
        aibiSession?.manualCopyPrompt()
        return prompt
    }

    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
        countdownTracker.reset()
        taskJob?.cancel()
        taskJob = null
        aibiSession?.cancelCurrentTask()
        aibiSession = null
        if (_state.value !is QuickValueRunState.Success) {
            _state.value = QuickValueRunState.Idle
        }
    }

    fun reset() {
        cancel()
        _state.value = QuickValueRunState.Idle
    }

    fun injectRunningState(
        phase: AIBIPhase,
        elapsedSeconds: Double? = null,
        statusMessage: String,
        sentPhotoCount: Int,
        excludedSimilarCount: Int = 0,
        excludedLimitCount: Int = 0
    ) {
        cancel()
        _state.value = QuickValueRunState.Running(
            phase = phase,
            elapsedSeconds = elapsedSeconds,
            statusMessage = statusMessage,
            sentPhotoCount = sentPhotoCount,
            excludedSimilarCount = excludedSimilarCount,
            excludedLimitCount = excludedLimitCount
        )
    }

    fun injectSuccessState(
        result: QuickValueResult,
        sentPhotoCount: Int = 6,
        excludedSimilarCount: Int = 0,
        excludedLimitCount: Int = 0
    ) {
        cancel()
        _state.value = QuickValueRunState.Success(
            result = result,
            sentPhotoCount = sentPhotoCount,
            excludedSimilarCount = excludedSimilarCount,
            excludedLimitCount = excludedLimitCount
        )
    }

    fun injectErrorState(message: String) {
        cancel()
        _state.value = QuickValueRunState.Error(message)
    }

    fun injectTimeoutState() {
        cancel()
        _state.value = QuickValueRunState.Timeout
    }

    val currentSession: AIBISession?
        get() = aibiSession

    companion object {
        fun isCountdownEligiblePhase(phase: AIBIPhase): Boolean =
            QuickValueCountdownTracker.isCountdownEligiblePhase(phase)
    }
}
