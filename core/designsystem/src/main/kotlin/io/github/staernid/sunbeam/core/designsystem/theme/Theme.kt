package io.github.staernid.sunbeam.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalOledBlack = staticCompositionLocalOf { false }

@Composable
fun SunbeamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    oledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && oledBlack) {
                base.copy(
                    background = SunbeamOledColorScheme.background,
                    surface = SunbeamOledColorScheme.surface,
                    surfaceVariant = SunbeamOledColorScheme.surfaceVariant,
                )
            } else {
                base
            }
        }
        darkTheme -> if (oledBlack) SunbeamOledColorScheme else SunbeamDarkColorScheme
        else -> SunbeamLightColorScheme
    }

    CompositionLocalProvider(
        LocalOledBlack provides (darkTheme && oledBlack),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SunbeamTypography,
            shapes = SunbeamShapes,
            content = content,
        )
    }
}
