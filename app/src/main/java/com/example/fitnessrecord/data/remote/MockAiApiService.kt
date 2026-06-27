package com.example.fitnessrecord.data.remote

import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiAdviceRequest
import com.example.fitnessrecord.model.NextWeekSuggestion
import kotlinx.coroutines.delay

class MockAiApiService : ApiService {
    override suspend fun requestAiAdvice(request: AiAdviceRequest): AiAdvice {
        delay(350)
        val records = request.records
        val totalDays = records.size
        val totalMinutes = records.mapNotNull { it.durationMinutes }.sum()
        val riskNotes = records.filter { record ->
            riskKeywords.any { keyword -> record.notes.contains(keyword, ignoreCase = true) }
        }
        val hasRisk = riskNotes.isNotEmpty()

        val summary = when {
            totalDays == 0 -> "本月暂无可分析的训练记录。"
            totalMinutes > 0 -> "本月记录了 $totalDays 天训练，已填写的总训练时长为 $totalMinutes 分钟。"
            else -> "本月记录了 $totalDays 天训练，但训练时长数据尚未完整填写。"
        }

        val frequencyAnalysis = when {
            totalDays == 0 -> "当前没有出勤数据，暂时无法判断训练频率。"
            totalDays >= 12 -> "本月出勤频率较高，建议继续关注恢复质量，避免连续高强度训练。"
            totalDays >= 6 -> "本月出勤比较稳定，可以优先保持节奏，再逐步增加训练量。"
            else -> "本月出勤偏少，建议先建立每周 2-3 次的固定训练节奏。"
        }

        val recoveryAdvice = buildList {
            add("训练后安排 5-10 分钟整理放松，并记录主观疲劳程度。")
            if (totalDays >= 10) {
                add("本月训练天数较多，建议每周至少安排 1-2 天低强度或休息日。")
            } else {
                add("优先保持规律训练，不必急于增加重量或组数。")
            }
            if (hasRisk) {
                add("备注中出现疼痛、受伤、头晕或胸闷相关内容，近期应降低训练强度。")
            }
        }

        val nextWeekPlan = if (hasRisk) {
            listOf(
                NextWeekSuggestion("周一", "暂停高强度训练，观察身体状态，必要时咨询医生。"),
                NextWeekSuggestion("周三", "如无不适，可做轻量活动或拉伸，不追求重量和强度。"),
                NextWeekSuggestion("周五", "根据恢复情况决定是否恢复训练，优先选择低风险动作。")
            )
        } else {
            listOf(
                NextWeekSuggestion("周一", "安排一次力量训练，选择 4-6 个熟悉动作，每个动作 2-4 组。"),
                NextWeekSuggestion("周三", "进行轻中等强度训练或有氧，控制在可持续完成的强度。"),
                NextWeekSuggestion("周五", "复盘本周记录，保留表现稳定的动作，避免一次性大幅加量。")
            )
        }

        val riskWarnings = buildList {
            if (hasRisk) {
                add("记录备注中出现风险信号：${riskNotes.joinToString("、") { it.date }}。请停止高强度训练并咨询医生。")
            }
            add("AI 建议不能替代医生诊断或专业教练评估。")
        }

        return AiAdvice(
            summary = summary,
            frequencyAnalysis = frequencyAnalysis,
            recoveryAdvice = recoveryAdvice,
            nextWeekPlan = nextWeekPlan,
            riskWarnings = riskWarnings,
            motivation = if (totalDays == 0) {
                "先完成一次真实记录，就是开始建立习惯。"
            } else {
                "你已经在用数据照顾自己的训练节奏，继续稳稳推进。"
            }
        )
    }

    private companion object {
        val riskKeywords = listOf("疼痛", "痛", "受伤", "伤", "头晕", "胸闷", "眩晕", "麻木", "不适")
    }
}
