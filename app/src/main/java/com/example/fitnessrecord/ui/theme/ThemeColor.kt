package com.example.fitnessrecord.ui.theme

import androidx.compose.ui.graphics.Color

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
