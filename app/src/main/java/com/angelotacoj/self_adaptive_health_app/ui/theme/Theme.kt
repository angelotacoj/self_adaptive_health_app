package com.angelotacoj.self_adaptive_health_app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CalmBlue80,
    secondary = Sage80,
    tertiary = WarmSand80
)

private val LightColorScheme = lightColorScheme(
    primary = CalmBlue40,
    secondary = Sage40,
    tertiary = WarmSand40,
    background = androidx.compose.ui.graphics.Color(0xFFF5F8F6),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE3ECE9),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFDDF3EE),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE8F3E6),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFF5E5),
    error = androidx.compose.ui.graphics.Color(0xFFBA4A4A)
)

@Composable
fun Self_Adaptive_Health_AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
