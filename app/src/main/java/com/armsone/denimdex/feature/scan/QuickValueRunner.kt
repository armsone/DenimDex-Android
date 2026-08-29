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
        val elapsedSeconds: Double,
        val statusMessage: String,
        val sentPhotoCount: Int,
        val excludedSimilarCount: Int,
        val excludedLimitCount: Int
    ) : QuickValueRunState()
    data class Success(
        val result: QuickValueResult,
        val sentPhotoCount: Int,
        val excludedSimilarCount: Int,
        val excludedLimitCount: Int
    ) : QuickValueRunState()
    data class Error(val message: String) : QuickValueRunState()
    object Timeout : QuickValueRunState()
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

    private var runStartTimeMs: Long = 0L
    private var activeSentRoles: List<String> = emptyList()

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
                        _state.value = QuickValueRunState.Error("가치 분석 결과를 해석하지 못했습니다: $err")
                        Result.failure(validated.exceptionOrNull() ?: Exception(err))
                    }
                }
            }

            runStartTimeMs = System.currentTimeMillis()
            startHostCountdown(sentCount, similarExcluded, limitExcluded)

            // Listen to session progress
            launch {
                session.progress.collect { prog ->
                    if (_state.value is QuickValueRunState.Running) {
                        val elapsed = (System.currentTimeMillis() - runStartTimeMs) / 1000.0
                        if (CountdownFormatter.isExpired(elapsed)) {
                            _state.value = QuickValueRunState.Timeout
                            session.cancelCurrentTask()
                        } else {
                            _state.value = QuickValueRunState.Running(
                                phase = prog.phase,
                                elapsedSeconds = elapsed,
                                statusMessage = prog.statusMessage,
                                sentPhotoCount = sentCount,
                                excludedSimilarCount = similarExcluded,
                                excludedLimitCount = limitExcluded
                            )
                        }
                    }
                }
            }

            // Listen to session errors
            launch {
                session.lastErrorMessage.collect { err ->
                    if (err != null && _state.value !is QuickValueRunState.Success) {
                        _state.value = QuickValueRunState.Error(err)
                    }
                }
            }

            _state.value = QuickValueRunState.Running(
                phase = AIBIPhase.INITIALIZING,
                elapsedSeconds = 0.0,
                statusMessage = "ChatGPT에 연결하고 있습니다...",
                sentPhotoCount = sentCount,
                excludedSimilarCount = similarExcluded,
                excludedLimitCount = limitExcluded
            )

            session.startTask(task, config, hiddenContainer)
        }
    }

    private fun startHostCountdown(
        sentCount: Int,
        similarExcluded: Int,
        limitExcluded: Int
    ) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (isActive) {
                delay(300L)
                val elapsed = (System.currentTimeMillis() - runStartTimeMs) / 1000.0
                if (CountdownFormatter.isExpired(elapsed)) {
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
        elapsedSeconds: Double,
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
}
