package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.model.AiAdviceResult
import com.example.fitnessrecord.model.AiDashboardData

interface AiAdviceRepository {
    suspend fun generateAdvice(): AiAdviceResult
    suspend fun getDashboardData(): AiDashboardData
}
