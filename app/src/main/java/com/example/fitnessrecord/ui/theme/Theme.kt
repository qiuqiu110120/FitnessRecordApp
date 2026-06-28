package com.example.fitnessrecord.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun FitnessRecordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeColorKey: String = "green",
    content: @Composable () -> Unit,
) {
    val seed = themeSeedColor(themeColorKey)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme(
            primary = seed.lighten(0.22f),
            secondary = seed.lighten(0.12f),
            tertiary = seed.lighten(0.36f),
            primaryContainer = seed.darken(0.28f),
            secondaryContainer = seed.darken(0.36f),
            tertiaryContainer = seed.darken(0.44f),
            surfaceTint = seed.lighten(0.22f)
        )

        else -> lightColorScheme(
            primary = seed,
            secondary = seed.darken(0.08f),
            tertiary = seed.lighten(0.18f),
            primaryContainer = seed.lighten(0.82f),
            secondaryContainer = seed.lighten(0.88f),
            tertiaryContainer = seed.lighten(0.76f),
            surfaceTint = seed
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun Color.lighten(amount: Float): Color = blend(Color.White, amount)

private fun Color.darken(amount: Float): Color = blend(Color.Black, amount)

private fun Color.blend(target: Color, amount: Float): Color = Color(
    red = red + (target.red - red) * amount,
    green = green + (target.green - green) * amount,
    blue = blue + (target.blue - blue) * amount,
    alpha = alpha
)
