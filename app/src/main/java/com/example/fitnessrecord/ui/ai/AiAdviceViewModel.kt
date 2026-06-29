package com.example.fitnessrecord.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.data.repository.AiAdviceRepository
import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiDashboardData
import com.example.fitnessrecord.model.AiTokenUsage
import com.example.fitnessrecord.util.AppLogger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val AI_TIMEOUT_SECONDS = 180

class AiAdviceViewModel(
    private val aiAdviceRepository: AiAdviceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AiAdviceUiState(isDashboardLoading = true))
    val uiState: StateFlow<AiAdviceUiState> = _uiState.asStateFlow()

    init {
        refreshDashboardData(showLoading = true)
    }

    fun refreshDashboardData(showLoading: Boolean = false) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isDashboardLoading = true)
            }
            AppLogger.i("AiAdvice", "Refreshing local AI dashboard data")
            runCatching {
                val dashboardData = aiAdviceRepository.getDashboardData()
                val cachedAdvice = aiAdviceRepository.getCachedAdvice(dashboardData)
                dashboardData to cachedAdvice
            }
                .onSuccess { (dashboardData, cachedAdvice) ->
                    val previousDashboard = _uiState.value.dashboardData
                    val dashboardChanged = previousDashboard != null && previousDashboard != dashboardData
                    _uiState.value = _uiState.value.copy(
                        isDashboardLoading = false,
                        dashboardData = dashboardData,
                        advice = cachedAdvice?.advice ?: if (dashboardChanged) null else _uiState.value.advice,
                        tokenUsage = cachedAdvice?.tokenUsage ?: if (dashboardChanged) null else _uiState.value.tokenUsage,
                        errorMessage = null,
                        eventMessage = when {
                            cachedAdvice != null && _uiState.value.advice == null -> "已加载上次AI建议"
                            dashboardChanged -> "训练数据已更新，请重新获取AI建议"
                            else -> _uiState.value.eventMessage
                        }
                    )
                }
                .onFailure { error ->
                    AppLogger.e("AiAdvice", "Local AI dashboard data refresh failed", error)
                    _uiState.value = _uiState.value.copy(
                        isDashboardLoading = false,
                        errorMessage = error.message ?: "读取本地训练统计失败。"
                    )
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            AppLogger.i("AiAdvice", "AI advice refresh started by user")
            startCountdown()
            runCatching {
                val dashboardData = aiAdviceRepository.getDashboardData()
                val result = withTimeout(AI_TIMEOUT_SECONDS * 1_000L) {
                    aiAdviceRepository.generateAdvice()
                }
                result to dashboardData
            }
                .onSuccess { (result, dashboardData) ->
                    AppLogger.i("AiAdvice", "AI advice refresh succeeded. totalTokens=${result.tokenUsage?.totalTokens}")
                    _uiState.value = AiAdviceUiState(
                        advice = result.advice,
                        dashboardData = dashboardData,
                        tokenUsage = result.tokenUsage,
                        eventMessage = "AI建议已生成并保存"
                    )
                }
                .onFailure { error ->
                    AppLogger.e("AiAdvice", "AI advice refresh failed", error)
                    val dashboardData = _uiState.value.dashboardData
                        ?: runCatching { aiAdviceRepository.getDashboardData() }.getOrNull()
                    val message = when (error) {
                        is TimeoutCancellationException -> "生成超时。连通性测试只验证短回复，完整建议可能需要更久；请检查模型响应速度、代理超时设置，或稍后重试。"
                        else -> error.message ?: "生成建议失败，请检查大模型配置和网络状态。"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isDashboardLoading = false,
                        progress = 0f,
                        remainingSeconds = AI_TIMEOUT_SECONDS,
                        dashboardData = dashboardData,
                        errorMessage = message,
                        eventMessage = "获取AI建议失败：$message"
                    )
                }
        }
    }

    fun consumeEventMessage() {
        _uiState.value = _uiState.value.copy(eventMessage = null)
    }

    private fun startCountdown() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isDashboardLoading = false,
            loadingMessage = "正在提交训练数据给 AI",
            progress = 0f,
            remainingSeconds = AI_TIMEOUT_SECONDS,
            errorMessage = null,
            eventMessage = null
        )
        viewModelScope.launch {
            for (second in AI_TIMEOUT_SECONDS downTo 0) {
                if (!_uiState.value.isLoading) return@launch
                val elapsed = AI_TIMEOUT_SECONDS - second
                _uiState.value = _uiState.value.copy(
                    progress = elapsed.toFloat() / AI_TIMEOUT_SECONDS.toFloat(),
                    remainingSeconds = second
                )
                delay(1_000L)
            }
        }
    }
}

data class AiAdviceUiState(
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
)
