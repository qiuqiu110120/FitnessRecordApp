package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.model.AiAdviceResult
import com.example.fitnessrecord.model.AiDashboardData
import com.example.fitnessrecord.model.AnalysisDataSnapshot
import com.example.fitnessrecord.model.AnalysisRequest
import kotlinx.coroutines.flow.Flow

interface AiAdviceRepository {
    fun observeAnalysisSnapshots(request: AnalysisRequest): Flow<AnalysisDataSnapshot>
    suspend fun loadAnalysisSnapshot(request: AnalysisRequest): AnalysisDataSnapshot
    suspend fun generateAdvice(snapshot: AnalysisDataSnapshot, requestId: String): AiAdviceResult
    suspend fun getDashboardData(snapshot: AnalysisDataSnapshot): AiDashboardData
    suspend fun clearPersistedAdviceCache()
}
