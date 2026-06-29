package com.example.fitnessrecord.data.remote

import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiAdviceRequest
import com.example.fitnessrecord.model.AiAdviceResult
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.model.AiTokenUsage
import com.example.fitnessrecord.model.NextWeekSuggestion
import com.example.fitnessrecord.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
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
    override suspend fun requestAiAdvice(request: AiAdviceRequest): AiAdviceResult = withContext(Dispatchers.IO) {
        validateConfig()
        val response = executeChatCompletion(
            ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = json.encodeToString(request.toPromptPayload()))
                )
            )
        )
        val content = response.firstContent().stripJsonFence()
        val advice = runCatching { json.decodeFromString<AiAdviceResponse>(content).toModel() }
            .getOrElse { error ->
                AppLogger.e("AiApi", "AI advice JSON parse failed. Content=${content.take(500)}", error)
                throw IOException("大模型返回 JSON 解析失败：${error.message}", error)
            }

        AiAdviceResult(advice = advice, tokenUsage = response.usage?.toModel())
    }

    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        validateConfig()
        val response = executeChatCompletion(
            ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    ChatMessage(role = "system", content = "你正在进行接口连通性测试。请只回复 OK。"),
                    ChatMessage(role = "user", content = "请回复 OK")
                ),
                temperature = 0.0
            )
        )
        val content = response.firstContent()
        if (content.isBlank()) {
            throw IOException("大模型返回内容为空")
        }
        "连接成功，模型已返回：${content.take(30)}"
    }

    private fun validateConfig() {
        require(config.baseUrl.isNotBlank()) { "请先填写大模型 Base URL" }
        require(config.apiKey.isNotBlank()) { "请先填写 API Key" }
        require(config.model.isNotBlank()) { "请先填写模型名称" }
    }

    private fun executeChatCompletion(payload: ChatCompletionRequest): ChatCompletionResponse {
        val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
        val primaryUrl = config.baseUrl.toChatCompletionsUrl()
        AppLogger.i("AiApi", "Requesting chat completion. url=$primaryUrl, model=${config.model}")
        return runCatching {
            executeChatCompletionRequest(primaryUrl, requestBody)
        }.recoverCatching { error ->
            val fallbackUrl = primaryUrl.httpsToHttpFallback()
            if (error.isTlsProtocolMismatch() && fallbackUrl != null) {
                AppLogger.i("AiApi", "TLS mismatch detected. Retrying with fallback url=$fallbackUrl")
                executeChatCompletionRequest(fallbackUrl, requestBody)
            } else {
                throw error.toUserFriendlyNetworkError(config.baseUrl)
            }
        }.getOrElse { error ->
            AppLogger.e("AiApi", "Chat completion request failed. baseUrl=${config.baseUrl}, model=${config.model}", error)
            throw error.toUserFriendlyNetworkError(config.baseUrl)
        }
    }

    private fun executeChatCompletionRequest(
        url: String,
        requestBody: okhttp3.RequestBody,
    ): ChatCompletionResponse {
        val httpRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                AppLogger.e("AiApi", "Chat completion failed. HTTP ${response.code}. Body=${body.take(300)}")
                throw IOException("大模型请求失败：HTTP ${response.code} ${body.take(180)}")
            }
            AppLogger.i("AiApi", "Chat completion succeeded. HTTP ${response.code}. BodyLength=${body.length}")
            return runCatching { json.decodeFromString<ChatCompletionResponse>(body) }
                .getOrElse { error ->
                    AppLogger.e("AiApi", "Chat completion response parse failed. Body=${body.take(500)}", error)
                    throw error
                }
        }
    }

    private fun ChatCompletionResponse.firstContent(): String {
        return choices.firstOrNull()?.message?.content?.stripJsonFence().orEmpty()
    }

    private fun String.toChatCompletionsUrl(): String {
        val trimmed = trim()
            .withDefaultScheme()
            .trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/chat/completions"
    }

    private fun String.withDefaultScheme(): String {
        val trimmed = trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun String.httpsToHttpFallback(): String? =
        if (startsWith("https://", ignoreCase = true)) {
            replaceFirst(Regex("^https://", RegexOption.IGNORE_CASE), "http://")
        } else {
            null
        }

    private fun Throwable.isTlsProtocolMismatch(): Boolean {
        val text = buildString {
            append(message.orEmpty())
            cause?.message?.let { append(' ').append(it) }
        }.lowercase()
        return "unable to parse tls packet header" in text ||
            "not an ssl/tls record" in text ||
            "wrong version number" in text ||
            "plaintext connection" in text
    }

    private fun Throwable.toUserFriendlyNetworkError(baseUrl: String): Throwable {
        if (isTlsProtocolMismatch()) {
            val suggestion = if (baseUrl.trim().startsWith("https://", ignoreCase = true)) {
                "当前 Base URL 使用 HTTPS，但目标服务像是明文 HTTP。请把 Base URL 改成 http:// 开头后重试。"
            } else {
                "目标服务的 HTTP/HTTPS 协议不匹配，请确认 Base URL 协议、端口和代理配置。"
            }
            return IOException(suggestion, this)
        }
        return this
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
                notes = record.notes,
                actions = record.actions.map { action ->
                    AiPromptAction(
                        name = action.name,
                        sets = action.sets.map { set ->
                            AiPromptSet(reps = set.reps, weightKg = set.weightKg)
                        }
                    )
                }
            )
        },
        attendanceTrend = attendanceTrend.map { point ->
            AiPromptTrendPoint(label = point.label, count = point.count)
        }
    )

    private companion object {
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val defaultClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
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
    val usage: TokenUsageResponse? = null,
)

@Serializable
private data class ChatChoice(
    val message: ChatMessage,
)

@Serializable
private data class TokenUsageResponse(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("total_tokens")
    val totalTokens: Int? = null,
) {
    fun toModel(): AiTokenUsage = AiTokenUsage(
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens
    )
}

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
    val actions: List<AiPromptAction>,
)

@Serializable
private data class AiPromptAction(
    val name: String,
    val sets: List<AiPromptSet>,
)

@Serializable
private data class AiPromptSet(
    val reps: Int?,
    val weightKg: Double?,
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
