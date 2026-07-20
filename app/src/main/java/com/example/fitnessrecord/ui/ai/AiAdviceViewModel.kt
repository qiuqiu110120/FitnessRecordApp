package com.example.fitnessrecord.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.data.repository.AiAdviceRepository
import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiDashboardData
import com.example.fitnessrecord.model.AiTokenUsage
import com.example.fitnessrecord.model.AnalysisDataSnapshot
import com.example.fitnessrecord.model.AnalysisRangePreset
import com.example.fitnessrecord.model.AnalysisRequest
import com.example.fitnessrecord.util.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Clock
import java.util.UUID

private const val AI_TIMEOUT_SECONDS = 180

class AiAdviceViewModel(
    private val aiAdviceRepository: AiAdviceRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AiAdviceUiState(
            isDashboardLoading = true,
            rangePreset = AnalysisRangePreset.Last90,
        )
    )
    val uiState: StateFlow<AiAdviceUiState> = _uiState.asStateFlow()
    val tokenUsage: StateFlow<AiTokenUsage?> = uiState
        .map { it.tokenUsage }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var snapshotJob: Job? = null
    private var adviceJob: Job? = null
    private var countdownJob: Job? = null

    // Keep generated advice for each range during this ViewModel session.
    // The snapshot id prevents advice from being restored after local data changes.
    private val adviceDrafts = MutableStateFlow<Map<AnalysisRangePreset, CachedAdvice>>(emptyMap())
    private val latestGenerationIds = MutableStateFlow<Map<AnalysisRangePreset, String>>(emptyMap())

    init {
        viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            aiAdviceRepository.clearPersistedAdviceCache()
        }
        observeRequest(createRequest(AnalysisRangePreset.Last90), showLoading = true)
    }

    fun selectRange(preset: AnalysisRangePreset) {
        if (_uiState.value.isLoading) return
        observeRequest(createRequest(preset), showLoading = true)
    }

    fun refreshDashboardData(showLoading: Boolean = false) {
        if (_uiState.value.isLoading) return
        observeRequest(createRequest(_uiState.value.rangePreset), showLoading = showLoading)
    }

    fun refresh() {
        val state = _uiState.value
        if (state.isLoading) return

        val request = state.request ?: createRequest(state.rangePreset)
        val observationRequestId = request.requestId
        val preset = presetFor(request)
        adviceJob?.cancel()
        adviceJob = viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            val snapshot = try {
                state.snapshot ?: aiAdviceRepository.loadAnalysisSnapshot(request)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update { current ->
                    if (current.request?.requestId != observationRequestId) {
                        current
                    } else {
                        current.copy(
                            isDashboardLoading = false,
                            errorMessage = error.message ?: "读取本地训练统计失败。",
                            aiState = AiRequestState.Error(null, null, error.message),
                        )
                    }
                }
                return@launch
            }

            val currentState = _uiState.value
            if (
                currentState.request?.requestId != observationRequestId ||
                (currentState.snapshotId != null && currentState.snapshotId != snapshot.snapshotId)
            ) {
                return@launch
            }

            val requestId = UUID.randomUUID().toString()
            latestGenerationIds.update { it + (preset to requestId) }
            if (!startCountdown(observationRequestId, requestId, snapshot)) {
                latestGenerationIds.update { ids ->
                    if (ids[preset] == requestId) ids - preset else ids
                }
                return@launch
            }

            try {
                val result = withTimeout(AI_TIMEOUT_SECONDS * 1_000L) {
                    aiAdviceRepository.generateAdvice(snapshot, requestId)
                }
                val latestSnapshot = aiAdviceRepository.loadAnalysisSnapshot(snapshot.request)
                if (latestGenerationIds.value[preset] != requestId) return@launch
                if (latestSnapshot.snapshotId != snapshot.snapshotId) {
                    invalidateForNewSnapshot(latestSnapshot)
                    return@launch
                }

                adviceDrafts.update { drafts ->
                    drafts + (
                        preset to CachedAdvice(
                            snapshotId = snapshot.snapshotId,
                            requestId = requestId,
                            advice = result.advice,
                            tokenUsage = result.tokenUsage,
                        )
                    )
                }
                _uiState.update { current ->
                    if (
                        current.request?.requestId != observationRequestId ||
                        current.activeRequestId != requestId ||
                        current.snapshotId != snapshot.snapshotId
                    ) {
                        current
                    } else {
                        current.copy(
                            isLoading = false,
                            isDashboardLoading = false,
                            advice = result.advice,
                            tokenUsage = result.tokenUsage,
                            errorMessage = null,
                            eventMessage = "AI建议已生成（${snapshot.request.range.displayLabel()}）",
                            aiState = AiRequestState.Success(requestId, snapshot.snapshotId),
                        )
                    }
                }
            } catch (error: CancellationException) {
                if (error is TimeoutCancellationException) {
                    handleAdviceFailure(requestId, snapshot.snapshotId, error)
                }
            } catch (error: Throwable) {
                handleAdviceFailure(requestId, snapshot.snapshotId, error)
            }
        }
    }

    fun consumeEventMessage() {
        _uiState.update { it.copy(eventMessage = null) }
    }

    private fun observeRequest(request: AnalysisRequest, showLoading: Boolean) {
        snapshotJob?.cancel()
        adviceJob?.cancel()
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                rangePreset = AnalysisRangePreset.entries.firstOrNull { preset ->
                    preset.days == request.range.lengthDays
                } ?: it.rangePreset,
                request = request,
                activeRequestId = null,
                snapshot = null,
                snapshotId = null,
                dashboardData = null,
                advice = null,
                tokenUsage = null,
                isLoading = false,
                isDashboardLoading = showLoading,
                progress = 0f,
                remainingSeconds = AI_TIMEOUT_SECONDS,
                errorMessage = null,
                eventMessage = null,
                aiState = AiRequestState.Idle,
            )
        }
        snapshotJob = viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            aiAdviceRepository.observeAnalysisSnapshots(request).collectLatest { snapshot ->
                val dashboardData = aiAdviceRepository.getDashboardData(snapshot)
                val preset = presetFor(request)
                val cachedAdvice = adviceDrafts.value[preset]
                val matchingCachedAdvice = cachedAdvice?.takeIf { it.snapshotId == snapshot.snapshotId }
                if (cachedAdvice != null && matchingCachedAdvice == null) {
                    adviceDrafts.update { drafts ->
                        if (drafts[preset]?.snapshotId == cachedAdvice.snapshotId) drafts - preset else drafts
                    }
                }
                var shouldCancelAdvice = false
                var shouldInvalidateGeneration = false
                _uiState.update { current ->
                    if (current.request?.requestId != request.requestId) {
                        current
                    } else {
                        val changed = current.snapshotId != null && current.snapshotId != snapshot.snapshotId
                        val staleCachedAdvice = cachedAdvice != null && matchingCachedAdvice == null
                        shouldInvalidateGeneration = changed || staleCachedAdvice
                        shouldCancelAdvice = shouldInvalidateGeneration && current.isLoading
                        current.copy(
                            isDashboardLoading = false,
                            snapshot = snapshot,
                            snapshotId = snapshot.snapshotId,
                            dashboardData = dashboardData,
                            advice = when {
                                changed -> null
                                matchingCachedAdvice != null -> matchingCachedAdvice.advice
                                else -> current.advice
                            },
                            tokenUsage = when {
                                changed -> null
                                matchingCachedAdvice != null -> matchingCachedAdvice.tokenUsage
                                else -> current.tokenUsage
                            },
                            activeRequestId = if (
                                matchingCachedAdvice != null && !current.isLoading && current.advice == null
                            ) {
                                matchingCachedAdvice.requestId
                            } else {
                                current.activeRequestId
                            },
                            errorMessage = if (changed || staleCachedAdvice) null else current.errorMessage,
                            isLoading = if (changed || staleCachedAdvice) false else current.isLoading,
                            progress = if (changed || staleCachedAdvice) 0f else current.progress,
                            aiState = when {
                                changed || staleCachedAdvice -> {
                                    AiRequestState.Stale(current.activeRequestId, snapshot.snapshotId)
                                }
                                matchingCachedAdvice != null && current.advice == null && !current.isLoading -> {
                                    AiRequestState.Success(matchingCachedAdvice.requestId, snapshot.snapshotId)
                                }
                                else -> current.aiState
                            },
                            eventMessage = when {
                                changed || staleCachedAdvice -> "训练数据已更新，请重新获取AI建议"
                                else -> current.eventMessage
                            }
                        )
                    }
                }
                if (shouldInvalidateGeneration) {
                    latestGenerationIds.update { ids -> ids - preset }
                }
                if (shouldCancelAdvice) {
                    adviceJob?.cancel()
                    countdownJob?.cancel()
                }
            }
        }
    }

    private fun handleAdviceFailure(requestId: String, snapshotId: String, error: Throwable) {
        val message = when (error) {
            is TimeoutCancellationException -> "生成超时。请检查模型响应速度、代理超时设置，或稍后重试。"
            else -> error.message ?: "生成建议失败，请检查大模型配置和网络状态。"
        }
        _uiState.update { current ->
            if (current.activeRequestId != requestId || current.snapshotId != snapshotId) {
                current
            } else {
                current.copy(
                    isLoading = false,
                    isDashboardLoading = false,
                    progress = 0f,
                    remainingSeconds = AI_TIMEOUT_SECONDS,
                    errorMessage = message,
                    eventMessage = "获取AI建议失败：$message",
                    aiState = AiRequestState.Error(requestId, snapshotId, message),
                )
            }
        }
    }

    private fun invalidateForNewSnapshot(snapshot: AnalysisDataSnapshot) {
        val preset = presetFor(snapshot.request)
        adviceDrafts.update { drafts -> drafts - preset }
        latestGenerationIds.update { ids -> ids - preset }
        _uiState.update { current ->
            if (current.request?.requestId != snapshot.request.requestId) {
                current
            } else {
                current.copy(
                    isLoading = false,
                    isDashboardLoading = false,
                    snapshot = snapshot,
                    snapshotId = snapshot.snapshotId,
                    advice = null,
                    tokenUsage = null,
                    eventMessage = "训练数据已更新，请重新获取AI建议",
                    aiState = AiRequestState.Stale(current.activeRequestId, snapshot.snapshotId),
                )
            }
        }
    }

    private fun startCountdown(
        observationRequestId: String,
        requestId: String,
        snapshot: AnalysisDataSnapshot,
    ): Boolean {
        var started = false
        countdownJob?.cancel()
        _uiState.update { current ->
            if (
                current.request?.requestId != observationRequestId ||
                (current.snapshotId != null && current.snapshotId != snapshot.snapshotId)
            ) {
                current
            } else {
                started = true
                current.copy(
                    isLoading = true,
                    isDashboardLoading = false,
                    snapshot = snapshot,
                    snapshotId = snapshot.snapshotId,
                    activeRequestId = requestId,
                    loadingMessage = "正在提交已记录的训练数据给 AI",
                    progress = 0f,
                    remainingSeconds = AI_TIMEOUT_SECONDS,
                    errorMessage = null,
                    eventMessage = null,
                    aiState = AiRequestState.Loading(requestId, snapshot.snapshotId),
                )
            }
        }
        if (!started) return false
        countdownJob = viewModelScope.launch(AppLogger.coroutineExceptionHandler) {
            for (second in AI_TIMEOUT_SECONDS downTo 0) {
                val state = _uiState.value
                if (
                    !state.isLoading ||
                    state.activeRequestId != requestId ||
                    state.snapshotId != snapshot.snapshotId
                ) {
                    return@launch
                }
                val elapsed = AI_TIMEOUT_SECONDS - second
                _uiState.update {
                    if (it.activeRequestId != requestId || it.snapshotId != snapshot.snapshotId) it else it.copy(
                        progress = elapsed.toFloat() / AI_TIMEOUT_SECONDS.toFloat(),
                        remainingSeconds = second,
                    )
                }
                delay(1_000L)
            }
        }
        return true
    }

    private fun presetFor(request: AnalysisRequest): AnalysisRangePreset =
        AnalysisRangePreset.entries.firstOrNull { it.days == request.range.lengthDays }
            ?: error("Unsupported analysis range: ${request.range.lengthDays} days")

    private fun createRequest(preset: AnalysisRangePreset): AnalysisRequest =
        AnalysisRequest.create(preset = preset, clock = clock)
}

private data class CachedAdvice(
    val snapshotId: String,
    val requestId: String,
    val advice: AiAdvice,
    val tokenUsage: AiTokenUsage?,
)

data class AiAdviceUiState(
    val rangePreset: AnalysisRangePreset = AnalysisRangePreset.Last90,
    val request: AnalysisRequest? = null,
    val snapshot: AnalysisDataSnapshot? = null,
    val snapshotId: String? = null,
    val activeRequestId: String? = null,
    val isLoading: Boolean = false,
    val isDashboardLoading: Boolean = false,
    val loadingMessage: String = "正在整理训练数据",
    val progress: Float = 0f,
    val remainingSeconds: Int = AI_TIMEOUT_SECONDS,
    val advice: AiAdvice? = null,
    val dashboardData: AiDashboardData? = null,
    val tokenUsage: AiTokenUsage? = null,
    val errorMessage: String? = null,
    val eventMessage: String? = null,
    val aiState: AiRequestState = AiRequestState.Idle,
)

sealed interface AiRequestState {
    data object Idle : AiRequestState
    data class Loading(val requestId: String, val snapshotId: String) : AiRequestState
    data class Success(val requestId: String, val snapshotId: String) : AiRequestState
    data class Error(val requestId: String?, val snapshotId: String?, val message: String?) : AiRequestState
    data class Stale(val requestId: String?, val snapshotId: String) : AiRequestState
}
