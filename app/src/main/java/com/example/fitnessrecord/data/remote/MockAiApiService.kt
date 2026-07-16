package com.example.fitnessrecord.data.remote

import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiAdviceRequest
import com.example.fitnessrecord.model.AiAdviceResult
import com.example.fitnessrecord.model.AiTokenUsage
import com.example.fitnessrecord.model.NextWeekSuggestion
import kotlinx.coroutines.delay

class MockAiApiService : ApiService {
    override suspend fun requestAiAdvice(request: AiAdviceRequest): AiAdviceResult {
        delay(350)
        val totalDays = request.activeTrainingDays
        val totalMinutes = request.totalMinutes
        val rangeLabel = "${request.rangeStart} 至 ${request.rangeEnd}"

        val summary = when {
            totalDays == 0 -> "$rangeLabel 暂无可分析的已记录训练。"
            totalMinutes > 0 -> "$rangeLabel 记录了 $totalDays 个活跃训练日，已填写的总训练时长为 $totalMinutes 分钟。"
            else -> "$rangeLabel 记录了 $totalDays 个活跃训练日，但训练时长数据尚未完整填写。"
        }

        val frequencyAnalysis = when {
            totalDays == 0 -> "当前没有已记录的训练日，暂时无法判断训练频率。"
            request.weeklyRecordedFrequency >= 3.0 -> "当前范围内的已记录训练频率较高，建议继续保持节奏，避免连续高强度训练。"
            request.weeklyRecordedFrequency >= 1.5 -> "当前范围内的已记录训练频率比较稳定，可以优先保持节奏，再逐步增加训练量。"
            else -> "当前范围内的已记录训练频率偏低，建议先建立每周 2-3 次的固定记录节奏。"
        }

        val recoveryAdvice = buildList {
            add("训练后安排 5-10 分钟整理放松；当前没有结构化恢复数据，暂时无法判断恢复状态。")
            if (totalDays >= 10) {
                add("当前范围内活跃训练日较多，建议每周至少安排 1-2 天低强度或休息日。")
            } else {
                add("优先保持规律记录，不必急于增加重量或组数。")
            }
        }

        val nextWeekPlan = listOf(
            NextWeekSuggestion("周一", "安排一次可持续完成的训练，并完整记录训练时长或动作组数。"),
            NextWeekSuggestion("周三", "根据已记录的训练节奏安排轻中等强度训练或休息。"),
            NextWeekSuggestion("周五", "复盘本周记录，保持稳定节奏，避免一次性大幅增加训练量。")
        )

        return AiAdviceResult(
            advice = AiAdvice(
                summary = summary,
                frequencyAnalysis = frequencyAnalysis,
                recoveryAdvice = recoveryAdvice,
                nextWeekPlan = nextWeekPlan,
                riskWarnings = listOf("AI 建议不能替代医生诊断或专业教练评估。"),
                motivation = if (totalDays == 0) {
                    "先完成一次真实记录，就是开始建立习惯。"
                } else {
                    "你已经在用数据照顾自己的训练节奏，继续稳稳推进。"
                }
            ),
            tokenUsage = AiTokenUsage(
                promptTokens = 0,
                completionTokens = 0,
                totalTokens = 0
            )
        )
    }
}
