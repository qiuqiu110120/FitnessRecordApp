package com.example.fitnessrecord.data.importer

import com.example.fitnessrecord.model.QuickImportExercise
import com.example.fitnessrecord.model.QuickImportSet
import com.example.fitnessrecord.model.QuickImportWorkout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val QUICK_IMPORT_SCHEMA_VERSION = 1
const val QUICK_IMPORT_MAX_FILE_BYTES: Long = 5L * 1024L * 1024L

data class QuickImportParseResult(
    val workouts: List<QuickImportWorkout> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    val isSuccess: Boolean get() = errors.isEmpty()
}

fun parseQuickWorkoutImportJson(jsonText: String): QuickImportParseResult {
    val root = runCatching { Json.parseToJsonElement(jsonText) }
        .getOrElse { error ->
            return QuickImportParseResult(errors = listOf("JSON 解析失败：${error.message ?: "文件内容不是有效 JSON"}"))
        }
    val rootObject = root as? JsonObject
        ?: return QuickImportParseResult(errors = listOf("顶层必须是对象，并包含 schemaVersion 和 workouts。"))

    val errors = mutableListOf<String>()
    val schemaVersion = rootObject["schemaVersion"]?.jsonNumberIntOrNull()
    if (schemaVersion != QUICK_IMPORT_SCHEMA_VERSION) {
        errors += "schemaVersion 必须是 $QUICK_IMPORT_SCHEMA_VERSION。"
    }

    val workoutsElement = rootObject["workouts"]
    val workoutsArray = workoutsElement as? JsonArray
    if (workoutsArray == null || workoutsArray.isEmpty()) {
        errors += "workouts 必须是非空数组。"
        return QuickImportParseResult(errors = errors)
    }

    val workouts = workoutsArray.mapIndexedNotNull { workoutIndex, element ->
        val workoutObject = element as? JsonObject
        if (workoutObject == null) {
            errors += "第 ${workoutIndex + 1} 条训练必须是对象。"
            return@mapIndexedNotNull null
        }

        val date = when {
            !workoutObject.containsKey("date") -> {
                errors += "第 ${workoutIndex + 1} 条训练缺少 date。"
                null
            }
            else -> {
                val dateText = workoutObject.stringValue("date")
                if (dateText == null) {
                    errors += "第 ${workoutIndex + 1} 条训练 date 必须是 YYYY-MM-DD 字符串。"
                    null
                } else {
                    parseDate(dateText, workoutIndex, errors)
                }
            }
        }
        val title = workoutObject.stringValue("title")?.trim().orEmpty().ifBlank { null }
        val notes = workoutObject.stringValue("notes")?.trim().orEmpty().ifBlank { null }
        val exercisesArray = workoutObject["exercises"] as? JsonArray
        if (exercisesArray == null || exercisesArray.isEmpty()) {
            errors += "第 ${workoutIndex + 1} 条训练 exercises 必须是非空数组。"
        }
        val exercises = exercisesArray?.mapIndexedNotNull { exerciseIndex, exerciseElement ->
            parseExercise(exerciseElement, workoutIndex, exerciseIndex, errors)
        }.orEmpty()

        if (date == null || exercises.isEmpty()) {
            null
        } else {
            QuickImportWorkout(date = date, title = title, notes = notes, exercises = exercises)
        }
    }

    return QuickImportParseResult(
        workouts = if (errors.isEmpty()) workouts else emptyList(),
        errors = errors
    )
}

fun quickImportExampleJson(): String = """
{
  "schemaVersion": 1,
  "workouts": [
    {
      "date": "2026-06-30",
      "title": "胸肩三头",
      "notes": "状态不错",
      "exercises": [
        {
          "name": "卧推",
          "sets": [
            { "weightKg": 60, "reps": 10 },
            { "weightKg": 70, "reps": 8 },
            { "weightKg": 75, "reps": 6 }
          ]
        },
        {
          "name": "俯卧撑",
          "sets": [
            { "reps": 15 },
            { "reps": 12 }
          ]
        },
        {
          "name": "跑步",
          "sets": [
            { "distanceKm": 3, "durationSeconds": 1200 }
          ]
        }
      ]
    }
  ]
}
""".trimIndent()

fun quickImportAiPrompt(): String = """
请把我下面的健身记录整理成这个 App 支持的 JSON 快速导入格式。

要求：
1. 只输出 JSON，不要输出解释、Markdown 或代码块，不要使用 ```json 包裹。
2. 顶层必须包含 schemaVersion 和 workouts，不要只输出 workouts 数组。
3. 日期格式必须是 YYYY-MM-DD，且必须是真实日期。
4. 无法确定日期的训练不要放入 workouts。
5. 重量单位统一为 kg。
6. weightKg、reps、durationSeconds、distanceKm 必须输出为 JSON number，不要输出字符串。
7. 每次训练放在 workouts 数组里。
8. 每个动作包含 name 和 sets。
9. 如果是自重训练，可以只填写 reps。
10. 如果是计时动作，可以填写 durationSeconds。
11. 如果是有氧或距离类训练，可以填写 distanceKm，也可以同时填写 durationSeconds。
12. 不确定的重量、次数、距离或时长不要猜测，可以省略对应字段。
13. 每组至少包含 reps、durationSeconds、distanceKm 之一。
14. 输出结构必须符合下面格式：

${quickImportExampleJson()}

这是我的原始记录：
""".trimIndent()

private fun parseExercise(
    element: JsonElement,
    workoutIndex: Int,
    exerciseIndex: Int,
    errors: MutableList<String>,
): QuickImportExercise? {
    val exerciseObject = element as? JsonObject
    if (exerciseObject == null) {
        errors += "第 ${workoutIndex + 1} 条训练的第 ${exerciseIndex + 1} 个动作必须是对象。"
        return null
    }
    val name = exerciseObject.stringValue("name")?.trim()
    if (name.isNullOrBlank()) {
        errors += "第 ${workoutIndex + 1} 条训练的第 ${exerciseIndex + 1} 个动作 name 不能为空。"
    }
    val setsArray = exerciseObject["sets"] as? JsonArray
    if (setsArray == null || setsArray.isEmpty()) {
        errors += "第 ${workoutIndex + 1} 条训练的第 ${exerciseIndex + 1} 个动作 sets 必须是非空数组。"
    }
    val sets = setsArray?.mapIndexedNotNull { setIndex, setElement ->
        parseSet(setElement, workoutIndex, exerciseIndex, name.orEmpty(), setIndex, errors)
    }.orEmpty()
    return if (name.isNullOrBlank() || sets.isEmpty()) null else QuickImportExercise(name = name, sets = sets)
}

private fun parseSet(
    element: JsonElement,
    workoutIndex: Int,
    exerciseIndex: Int,
    exerciseName: String,
    setIndex: Int,
    errors: MutableList<String>,
): QuickImportSet? {
    val setObject = element as? JsonObject
    val location = setLocation(workoutIndex, exerciseIndex, exerciseName, setIndex)
    if (setObject == null) {
        errors += "$location 必须是对象。"
        return null
    }

    val reps = setObject.positiveInt("reps", location, errors)
    val weightKg = setObject.nonNegativeDouble("weightKg", location, errors)
    val durationSeconds = setObject.positiveInt("durationSeconds", location, errors)
    val distanceKm = setObject.positiveDouble("distanceKm", location, errors)
    val notes = setObject.stringValue("notes")?.trim().orEmpty()

    if (reps == null && durationSeconds == null && distanceKm == null) {
        errors += "$location 至少需要 reps、durationSeconds、distanceKm 之一，weightKg 不能单独构成有效训练组。"
    }

    return QuickImportSet(
        reps = reps,
        weightKg = weightKg,
        durationSeconds = durationSeconds,
        distanceKm = distanceKm,
        notes = notes
    )
}

private fun parseDate(value: String, workoutIndex: Int, errors: MutableList<String>): LocalDate? {
    val trimmed = value.trim()
    if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(trimmed)) {
        errors += "第 ${workoutIndex + 1} 条训练 date 必须是 YYYY-MM-DD。"
        return null
    }
    return try {
        LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        errors += "第 ${workoutIndex + 1} 条训练 date 必须是真实有效日期。"
        null
    }
}

private fun JsonObject.stringValue(key: String): String? {
    val value = this[key] ?: return null
    if (value is JsonNull) return null
    return (value as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
}

private fun JsonObject.positiveInt(key: String, location: String, errors: MutableList<String>): Int? {
    val value = this[key] ?: return null
    val primitive = value as? JsonPrimitive
    val number = primitive?.takeIf { !it.isString }?.intOrNull
    if (number == null || number <= 0) {
        errors += "$location $key 必须是正整数。"
        return null
    }
    return number
}

private fun JsonObject.nonNegativeDouble(key: String, location: String, errors: MutableList<String>): Double? {
    val value = this[key] ?: return null
    val primitive = value as? JsonPrimitive
    val number = primitive?.takeIf { !it.isString }?.doubleOrNull
    if (number == null || number < 0.0) {
        errors += "$location $key 必须是 0 或正数。"
        return null
    }
    return number
}

private fun JsonObject.positiveDouble(key: String, location: String, errors: MutableList<String>): Double? {
    val value = this[key] ?: return null
    val primitive = value as? JsonPrimitive
    val number = primitive?.takeIf { !it.isString }?.doubleOrNull
    if (number == null || number <= 0.0) {
        errors += "$location $key 必须是正数。"
        return null
    }
    return number
}

private fun JsonElement.jsonNumberIntOrNull(): Int? =
    (this as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull

private fun setLocation(workoutIndex: Int, exerciseIndex: Int, exerciseName: String, setIndex: Int): String {
    val namePart = exerciseName.trim().ifBlank { "第 ${exerciseIndex + 1} 个动作" }
    return "第 ${workoutIndex + 1} 条训练的“$namePart”第 ${setIndex + 1} 组"
}
