package io.github.staernid.sunbeam.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val SunbeamOrangePrimaryDark = Color(0xFFFB923C)
val SunbeamOrangeOnPrimaryDark = Color(0xFF3B1F0A)
val SunbeamOrangePrimaryContainerDark = Color(0xFF6E3900)
val SunbeamOrangeOnPrimaryContainerDark = Color(0xFFFFDCC5)
val SunbeamNeutralSecondaryDark = Color(0xFFD4D4D8)
val SunbeamNeutralOnSecondaryDark = Color(0xFF27272A)
val SunbeamNeutralSecondaryContainerDark = Color(0xFF3F3F46)
val SunbeamNeutralOnSecondaryContainerDark = Color(0xFFE4E4E7)

val SunbeamBackgroundDark = Color(0xFF18181B)
val SunbeamOnBackgroundDark = Color(0xFFF4F4F5)
val SunbeamSurfaceDark = Color(0xFF202023)
val SunbeamOnSurfaceDark = Color(0xFFF4F4F5)
val SunbeamSurfaceVariantDark = Color(0xFF27272A)
val SunbeamOnSurfaceVariantDark = Color(0xFFA1A1AA)
val SunbeamOutlineDark = Color(0xFF3F3F46)
val SunbeamOutlineVariantDark = Color(0xFF27272A)

val SunbeamOrangePrimaryLight = Color(0xFFC2410C)
val SunbeamOrangeOnPrimaryLight = Color(0xFFFFFFFF)
val SunbeamOrangePrimaryContainerLight = Color(0xFFFFEDD5)
val SunbeamOrangeOnPrimaryContainerLight = Color(0xFF4E2600)
val SunbeamNeutralSecondaryLight = Color(0xFF52525B)
val SunbeamNeutralOnSecondaryLight = Color(0xFFFFFFFF)
val SunbeamNeutralSecondaryContainerLight = Color(0xFFE4E4E7)
val SunbeamNeutralOnSecondaryContainerLight = Color(0xFF18181B)

val SunbeamBackgroundLight = Color(0xFFFAFAF9)
val SunbeamOnBackgroundLight = Color(0xFF18181B)
val SunbeamSurfaceLight = Color(0xFFFFFFFF)
val SunbeamOnSurfaceLight = Color(0xFF18181B)
val SunbeamSurfaceVariantLight = Color(0xFFF4F4F5)
val SunbeamOnSurfaceVariantLight = Color(0xFF52525B)
val SunbeamOutlineLight = Color(0xFFD4D4D8)
val SunbeamOutlineVariantLight = Color(0xFFE4E4E7)

val SunbeamDarkColorScheme = darkColorScheme(
    primary = SunbeamOrangePrimaryDark,
    onPrimary = SunbeamOrangeOnPrimaryDark,
    primaryContainer = SunbeamOrangePrimaryContainerDark,
    onPrimaryContainer = SunbeamOrangeOnPrimaryContainerDark,
    secondary = SunbeamNeutralSecondaryDark,
    onSecondary = SunbeamNeutralOnSecondaryDark,
    secondaryContainer = SunbeamNeutralSecondaryContainerDark,
    onSecondaryContainer = SunbeamNeutralOnSecondaryContainerDark,
    tertiary = SunbeamNeutralSecondaryDark,
    onTertiary = SunbeamNeutralOnSecondaryDark,
    tertiaryContainer = SunbeamNeutralSecondaryContainerDark,
    onTertiaryContainer = SunbeamNeutralOnSecondaryContainerDark,
    background = SunbeamBackgroundDark,
    onBackground = SunbeamOnBackgroundDark,
    surface = SunbeamSurfaceDark,
    onSurface = SunbeamOnSurfaceDark,
    surfaceVariant = SunbeamSurfaceVariantDark,
    onSurfaceVariant = SunbeamOnSurfaceVariantDark,
    outline = SunbeamOutlineDark,
    outlineVariant = SunbeamOutlineVariantDark,
)

val SunbeamOledColorScheme = SunbeamDarkColorScheme.copy(
    background = Color.Black,
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF141414),
    outline = Color(0xFF262626),
    outlineVariant = Color(0xFF181818),
)

val SunbeamLightColorScheme = lightColorScheme(
    primary = SunbeamOrangePrimaryLight,
    onPrimary = SunbeamOrangeOnPrimaryLight,
    primaryContainer = SunbeamOrangePrimaryContainerLight,
    onPrimaryContainer = SunbeamOrangeOnPrimaryContainerLight,
    secondary = SunbeamNeutralSecondaryLight,
    onSecondary = SunbeamNeutralOnSecondaryLight,
    secondaryContainer = SunbeamNeutralSecondaryContainerLight,
    onSecondaryContainer = SunbeamNeutralOnSecondaryContainerLight,
    tertiary = SunbeamNeutralSecondaryLight,
    onTertiary = SunbeamNeutralOnSecondaryLight,
    tertiaryContainer = SunbeamNeutralSecondaryContainerLight,
    onTertiaryContainer = SunbeamNeutralOnSecondaryContainerLight,
    background = SunbeamBackgroundLight,
    onBackground = SunbeamOnBackgroundLight,
    surface = SunbeamSurfaceLight,
    onSurface = SunbeamOnSurfaceLight,
    surfaceVariant = SunbeamSurfaceVariantLight,
    onSurfaceVariant = SunbeamOnSurfaceVariantLight,
    outline = SunbeamOutlineLight,
    outlineVariant = SunbeamOutlineVariantLight,
)
