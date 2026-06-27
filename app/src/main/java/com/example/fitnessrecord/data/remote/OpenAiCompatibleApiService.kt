package com.example.fitnessrecord.data.remote

import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiAdviceRequest
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.model.NextWeekSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAiCompatibleApiService(
    private val config: AiProviderConfig,
    private val client: OkHttpClient = defaultClient,
    private val json: Json = defaultJson,
) : ApiService {
    override suspend fun requestAiAdvice(request: AiAdviceRequest): AiAdvice = withContext(Dispatchers.IO) {
        require(config.baseUrl.isNotBlank()) { "请先填写大模型 Base URL" }
        require(config.apiKey.isNotBlank()) { "请先填写 API Key" }
        require(config.model.isNotBlank()) { "请先填写模型名称" }

        val httpRequest = Request.Builder()
            .url(config.baseUrl.toChatCompletionsUrl())
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(
                json.encodeToString(
                    ChatCompletionRequest(
                        model = config.model,
                        messages = listOf(
                            ChatMessage(role = "system", content = systemPrompt),
                            ChatMessage(role = "user", content = json.encodeToString(request.toPromptPayload()))
                        )
                    )
                ).toRequestBody(jsonMediaType)
            )
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("大模型请求失败：HTTP ${response.code} ${body.take(180)}")
            }

            val content = json.decodeFromString<ChatCompletionResponse>(body)
                .choices
                .firstOrNull()
                ?.message
                ?.content
                ?.stripJsonFence()
                .orEmpty()

            if (content.isBlank()) {
                throw IOException("大模型返回内容为空")
            }

            runCatching { json.decodeFromString<AiAdviceResponse>(content).toModel() }
                .getOrElse { error -> throw IOException("大模型返回 JSON 解析失败：${error.message}") }
        }
    }

    private fun String.toChatCompletionsUrl(): String {
        val trimmed = trim().trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/chat/completions"
    }

    private fun String.stripJsonFence(): String {
        val trimmed = trim()
        return trimmed
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun AiAdviceRequest.toPromptPayload(): AiPromptPayload = AiPromptPayload(
        records = records.map { record ->
            AiPromptRecord(
                date = record.date,
                trainingType = record.trainingType,
                durationMinutes = record.durationMinutes,
                notes = record.notes
            )
        },
        attendanceTrend = attendanceTrend.map { point ->
            AiPromptTrendPoint(label = point.label, count = point.count)
        }
    )

    private companion object {
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val defaultClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        val defaultJson = Json { ignoreUnknownKeys = true }
        val systemPrompt = """
            你是一个谨慎、专业的健身数据分析助手。

            我会给你用户的健身记录数据，包括日期、训练类型、训练时长、备注和最近出勤趋势。
            请根据数据生成结构化建议。

            要求：
            - 不要编造用户没有提供的数据
            - 不要给医疗诊断
            - 如果数据中出现疼痛、受伤、头晕、胸闷等内容，提醒用户停止高强度训练并咨询医生
            - 建议要具体、可执行、温和
            - 不要输出大段散文
            - 请严格返回 JSON，不要返回 Markdown

            返回格式：
            {
              "summary": "本月训练总结",
              "frequencyAnalysis": "训练频率分析",
              "recoveryAdvice": ["恢复建议1", "恢复建议2"],
              "nextWeekPlan": [
                {
                  "day": "周一",
                  "suggestion": "训练建议"
                }
              ],
              "riskWarnings": ["风险提醒1"],
              "motivation": "一句鼓励"
            }
        """.trimIndent()
    }
}

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.4,
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
private data class ChatChoice(
    val message: ChatMessage,
)

@Serializable
private data class AiPromptPayload(
    val records: List<AiPromptRecord>,
    val attendanceTrend: List<AiPromptTrendPoint>,
)

@Serializable
private data class AiPromptRecord(
    val date: String,
    val trainingType: String,
    val durationMinutes: Int?,
    val notes: String,
)

@Serializable
private data class AiPromptTrendPoint(
    val label: String,
    val count: Int,
)

@Serializable
private data class AiAdviceResponse(
    val summary: String = "",
    val frequencyAnalysis: String = "",
    val recoveryAdvice: List<String> = emptyList(),
    val nextWeekPlan: List<NextWeekSuggestionResponse> = emptyList(),
    val riskWarnings: List<String> = emptyList(),
    val motivation: String = "",
) {
    fun toModel(): AiAdvice = AiAdvice(
        summary = summary.ifBlank { "暂无足够数据生成本月总结。" },
        frequencyAnalysis = frequencyAnalysis.ifBlank { "暂无足够数据分析训练频率。" },
        recoveryAdvice = recoveryAdvice.ifEmpty { listOf("保持训练记录完整，便于后续分析恢复情况。") },
        nextWeekPlan = nextWeekPlan.map { it.toModel() },
        riskWarnings = riskWarnings.ifEmpty { listOf("AI 建议不能替代医生诊断或专业教练评估。") },
        motivation = motivation.ifBlank { "保持真实记录，稳步推进就很好。" }
    )
}

@Serializable
private data class NextWeekSuggestionResponse(
    val day: String = "",
    val suggestion: String = "",
) {
    fun toModel(): NextWeekSuggestion = NextWeekSuggestion(
        day = day.ifBlank { "待安排" },
        suggestion = suggestion.ifBlank { "根据身体状态安排低到中等强度训练。" }
    )
}
