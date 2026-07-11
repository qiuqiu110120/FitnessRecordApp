package com.example.fitnessrecord.ui.theme

import androidx.compose.ui.graphics.Color
import java.util.Locale

data class ThemeColorOption(
    val key: String,
    val label: String,
    val color: Color,
)

const val DefaultThemeColorKey = "green"

val ThemeColorOptions = listOf(
    ThemeColorOption(DefaultThemeColorKey, "松绿", Color(0xFF2E7D32)),
    ThemeColorOption("blue", "湖蓝", Color(0xFF1565C0)),
    ThemeColorOption("purple", "紫色", Color(0xFF6A1B9A)),
    ThemeColorOption("orange", "暖橙", Color(0xFFEF6C00)),
    ThemeColorOption("gray", "石墨", Color(0xFF546E7A)),
)

fun themeColorOption(key: String): ThemeColorOption =
    ThemeColorOptions.firstOrNull { it.key == key } ?: ThemeColorOptions.first()

fun themeSeedColor(value: String): Color =
    ThemeColorOptions.firstOrNull { it.key == value }?.color
        ?: decodePersistedThemeColor(value)?.let(::createThemeColor)
        ?: themeColorOption(DefaultThemeColorKey).color

fun normalizeThemeColorInput(value: String): String? =
    when (val result = parseThemeColorInput(value)) {
        is ThemeColorParseResult.Valid -> result.hex
        else -> null
    }

fun normalizeThemeColorSelectionForStorage(value: String): String? =
    ThemeColorOptions.firstOrNull { it.key == value }?.key ?: decodePersistedThemeColor(value)

fun decodePersistedThemeColor(value: String): String? =
    value.takeIf(::isCanonicalHexColor)

fun isCustomThemeColor(value: String): Boolean =
    ThemeColorOptions.none { it.key == value } && decodePersistedThemeColor(value) != null

internal sealed interface ThemeColorParseResult {
    data object Empty : ThemeColorParseResult
    data object TooLong : ThemeColorParseResult
    data object UnsupportedFormat : ThemeColorParseResult
    data object ChannelOutOfRange : ThemeColorParseResult
    data class Valid(val hex: String) : ThemeColorParseResult
}

internal fun parseThemeColorInput(value: String): ThemeColorParseResult {
    if (value.length > MaxRawInputLength) return ThemeColorParseResult.TooLong

    val input = value.trim(' ')
    if (input.isEmpty()) return ThemeColorParseResult.Empty
    if (input.length > MaxFormatInputLength) return ThemeColorParseResult.TooLong

    normalizeHexInput(input)?.let { return ThemeColorParseResult.Valid(it) }
    return parseRgbInput(input)
}

private fun normalizeHexInput(value: String): String? {
    val clean = if (value.startsWith('#')) value.drop(1) else value
    if (clean.length != 6 || !clean.all(::isAsciiHexDigit)) return null
    return "#${clean.uppercase(Locale.ROOT)}"
}

private fun parseRgbInput(value: String): ThemeColorParseResult {
    val match = RgbPattern.matchEntire(value) ?: return ThemeColorParseResult.UnsupportedFormat
    val channels = match.groupValues.drop(1).map { channel ->
        channel.toIntOrNull() ?: return ThemeColorParseResult.ChannelOutOfRange
    }
    if (channels.any { it !in 0..255 }) return ThemeColorParseResult.ChannelOutOfRange
    return ThemeColorParseResult.Valid(
        "#%02X%02X%02X".format(Locale.ROOT, channels[0], channels[1], channels[2])
    )
}

private fun createThemeColor(hex: String): Color {
    val rgb = hex.substring(1).toInt(16)
    return Color((0xFF shl 24) or rgb)
}

private fun isCanonicalHexColor(value: String): Boolean =
    value.length == 7 &&
        value[0] == '#' &&
        value.substring(1).all { it in '0'..'9' || it in 'A'..'F' }

private fun isAsciiHexDigit(value: Char): Boolean =
    value in '0'..'9' || value in 'a'..'f' || value in 'A'..'F'

private const val MaxRawInputLength = 128
private const val MaxFormatInputLength = 64
private val RgbPattern = Regex(
    """[rR][gG][bB]\( *([0-9]+) *, *([0-9]+) *, *([0-9]+) *\)"""
)
