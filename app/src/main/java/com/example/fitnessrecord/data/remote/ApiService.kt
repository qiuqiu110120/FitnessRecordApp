package com.example.fitnessrecord.data.remote

import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiAdviceRequest

interface ApiService {
    suspend fun requestAiAdvice(request: AiAdviceRequest): AiAdvice
}
