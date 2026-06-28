package com.example.fitnessrecord.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.data.repository.AiAdviceRepository
import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiDashboardData
import com.example.fitnessrecord.model.AiTokenUsage
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val AI_TIMEOUT_SECONDS = 20

class AiAdviceViewModel(
    private val aiAdviceRepository: AiAdviceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AiAdviceUiState(isLoading = true))
    val uiState: StateFlow<AiAdviceUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            startCountdown()
            val dashboardDeferred = async { aiAdviceRepository.getDashboardData() }
            runCatching {
                val result = withTimeout(AI_TIMEOUT_SECONDS * 1_000L) {
                    aiAdviceRepository.generateAdvice()
                }
                result to dashboardDeferred.await()
            }
                .onSuccess { (result, dashboardData) ->
                    _uiState.value = AiAdviceUiState(
                        advice = result.advice,
                        dashboardData = dashboardData,
                        tokenUsage = result.tokenUsage,
                        eventMessage = "AI建议已生成"
                    )
                }
                .onFailure { error ->
                    val dashboardData = runCatching { dashboardDeferred.await() }.getOrNull()
                    val message = when (error) {
                        is TimeoutCancellationException -> "生成超时，请检查网络、大模型 Base URL 或稍后重试。"
                        else -> error.message ?: "生成建议失败，请检查大模型配置和网络状态。"
                    }
                    _uiState.value = AiAdviceUiState(
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
    val loadingMessage: String = "正在整理训练数据",
    val progress: Float = 0f,
    val remainingSeconds: Int = AI_TIMEOUT_SECONDS,
    val advice: AiAdvice? = null,
    val dashboardData: AiDashboardData? = null,
    val tokenUsage: AiTokenUsage? = null,
    val errorMessage: String? = null,
    val eventMessage: String? = null,
)
