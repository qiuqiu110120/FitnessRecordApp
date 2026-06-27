package com.example.fitnessrecord.data.repository

import com.example.fitnessrecord.model.AiAdvice

interface AiAdviceRepository {
    suspend fun generateAdvice(): AiAdvice
}
