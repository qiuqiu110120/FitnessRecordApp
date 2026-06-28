package com.example.fitnessrecord.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

data class ThemeColorOption(
    val key: String,
    val label: String,
    val color: Color,
)

val ThemeColorOptions = listOf(
    ThemeColorOption("green", "松绿", Color(0xFF2E7D32)),
    ThemeColorOption("blue", "湖蓝", Color(0xFF1565C0)),
    ThemeColorOption("purple", "紫色", Color(0xFF6A1B9A)),
    ThemeColorOption("orange", "暖橙", Color(0xFFEF6C00)),
    ThemeColorOption("gray", "石墨", Color(0xFF546E7A)),
)

fun themeColorOption(key: String): ThemeColorOption =
    ThemeColorOptions.firstOrNull { it.key == key } ?: ThemeColorOptions.first()

fun themeSeedColor(value: String): Color =
    parseThemeColor(value) ?: themeColorOption(value).color

fun normalizeThemeColorInput(value: String): String? =
    parseThemeColor(value)?.toHexString()

fun isCustomThemeColor(value: String): Boolean =
    ThemeColorOptions.none { it.key == value } && parseThemeColor(value) != null

private fun parseThemeColor(value: String): Color? {
    val input = value.trim()
    if (input.isBlank()) return null

    parseHexColor(input)?.let { return it }
    parseRgbColor(input)?.let { return it }
    return null
}

private fun parseHexColor(value: String): Color? {
    val clean = value.removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    if (!clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null

    val colorLong = clean.toLongOrNull(16) ?: return null
    val argb = if (clean.length == 6) {
        0xFF000000L or colorLong
    } else {
        colorLong
    }
    return Color(argb.toULong())
}

private fun parseRgbColor(value: String): Color? {
    val content = value
        .removePrefix("rgb(")
        .removePrefix("RGB(")
        .removeSuffix(")")
    val parts = content.split(",").map { it.trim() }
    if (parts.size != 3) return null

    val red = parts[0].toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val green = parts[1].toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val blue = parts[2].toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    return Color(red, green, blue)
}

private fun Color.toHexString(): String {
    val red = (red * 255).roundToInt().coerceIn(0, 255)
    val green = (green * 255).roundToInt().coerceIn(0, 255)
    val blue = (blue * 255).roundToInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(red, green, blue)
}
