package com.example.fitnessrecord.data.remote

import com.example.fitnessrecord.model.AiAdviceResult
import com.example.fitnessrecord.model.AiAdviceRequest

interface ApiService {
    suspend fun requestAiAdvice(request: AiAdviceRequest): AiAdviceResult
}
