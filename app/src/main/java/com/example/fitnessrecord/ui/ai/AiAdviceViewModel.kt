package com.example.fitnessrecord.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessrecord.data.repository.AiAdviceRepository
import com.example.fitnessrecord.model.AiAdvice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { aiAdviceRepository.generateAdvice() }
                .onSuccess { advice -> _uiState.value = AiAdviceUiState(advice = advice) }
                .onFailure { error ->
                    _uiState.value = AiAdviceUiState(
                        errorMessage = error.message ?: "生成建议失败，请检查大模型配置和网络状态。"
                    )
                }
        }
    }
}

data class AiAdviceUiState(
    val isLoading: Boolean = false,
    val advice: AiAdvice? = null,
    val errorMessage: String? = null,
)
