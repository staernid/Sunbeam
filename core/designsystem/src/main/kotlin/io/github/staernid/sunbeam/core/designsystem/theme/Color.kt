package io.github.staernid.sunbeam.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val SunbeamAmberPrimaryDark = Color(0xFFFBBF24)
val SunbeamAmberOnPrimaryDark = Color(0xFF452B00)
val SunbeamAmberPrimaryContainerDark = Color(0xFF633F00)
val SunbeamAmberOnPrimaryContainerDark = Color(0xFFFFDEAC)

val SunbeamCyanSecondaryDark = Color(0xFF38BDF8)
val SunbeamCyanOnSecondaryDark = Color(0xFF003548)
val SunbeamCyanSecondaryContainerDark = Color(0xFF004D67)
val SunbeamCyanOnSecondaryContainerDark = Color(0xFFBAEAFF)

val SunbeamCoralTertiaryDark = Color(0xFFFB923C)
val SunbeamCoralOnTertiaryDark = Color(0xFF4E2600)
val SunbeamCoralTertiaryContainerDark = Color(0xFF6E3900)
val SunbeamCoralOnTertiaryContainerDark = Color(0xFFFFDCC5)

val SunbeamBackgroundDark = Color(0xFF0B0F19)
val SunbeamOnBackgroundDark = Color(0xFFE2E8F0)
val SunbeamSurfaceDark = Color(0xFF111827)
val SunbeamOnSurfaceDark = Color(0xFFE2E8F0)
val SunbeamSurfaceVariantDark = Color(0xFF1E293B)
val SunbeamOnSurfaceVariantDark = Color(0xFF94A3B8)
val SunbeamOutlineDark = Color(0xFF334155)
val SunbeamOutlineVariantDark = Color(0xFF1E293B)

val SunbeamAmberPrimaryLight = Color(0xFFB45309)
val SunbeamAmberOnPrimaryLight = Color(0xFFFFFFFF)
val SunbeamAmberPrimaryContainerLight = Color(0xFFFEF3C7)
val SunbeamAmberOnPrimaryContainerLight = Color(0xFF452B00)

val SunbeamCyanSecondaryLight = Color(0xFF0284C7)
val SunbeamCyanOnSecondaryLight = Color(0xFFFFFFFF)
val SunbeamCyanSecondaryContainerLight = Color(0xFFE0F2FE)
val SunbeamCyanOnSecondaryContainerLight = Color(0xFF003548)

val SunbeamCoralTertiaryLight = Color(0xFFC2410C)
val SunbeamCoralOnTertiaryLight = Color(0xFFFFFFFF)
val SunbeamCoralTertiaryContainerLight = Color(0xFFFFEDD5)
val SunbeamCoralOnTertiaryContainerLight = Color(0xFF4E2600)

val SunbeamBackgroundLight = Color(0xFFF8FAFC)
val SunbeamOnBackgroundLight = Color(0xFF0F172A)
val SunbeamSurfaceLight = Color(0xFFFFFFFF)
val SunbeamOnSurfaceLight = Color(0xFF0F172A)
val SunbeamSurfaceVariantLight = Color(0xFFF1F5F9)
val SunbeamOnSurfaceVariantLight = Color(0xFF64748B)
val SunbeamOutlineLight = Color(0xFFCBD5E1)
val SunbeamOutlineVariantLight = Color(0xFFE2E8F0)

val SunbeamDarkColorScheme = darkColorScheme(
    primary = SunbeamAmberPrimaryDark,
    onPrimary = SunbeamAmberOnPrimaryDark,
    primaryContainer = SunbeamAmberPrimaryContainerDark,
    onPrimaryContainer = SunbeamAmberOnPrimaryContainerDark,
    secondary = SunbeamCyanSecondaryDark,
    onSecondary = SunbeamCyanOnSecondaryDark,
    secondaryContainer = SunbeamCyanSecondaryContainerDark,
    onSecondaryContainer = SunbeamCyanOnSecondaryContainerDark,
    tertiary = SunbeamCoralTertiaryDark,
    onTertiary = SunbeamCoralOnTertiaryDark,
    tertiaryContainer = SunbeamCoralTertiaryContainerDark,
    onTertiaryContainer = SunbeamCoralOnTertiaryContainerDark,
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
    primary = SunbeamAmberPrimaryLight,
    onPrimary = SunbeamAmberOnPrimaryLight,
    primaryContainer = SunbeamAmberPrimaryContainerLight,
    onPrimaryContainer = SunbeamAmberOnPrimaryContainerLight,
    secondary = SunbeamCyanSecondaryLight,
    onSecondary = SunbeamCyanOnSecondaryLight,
    secondaryContainer = SunbeamCyanSecondaryContainerLight,
    onSecondaryContainer = SunbeamCyanOnSecondaryContainerLight,
    tertiary = SunbeamCoralTertiaryLight,
    onTertiary = SunbeamCoralOnTertiaryLight,
    tertiaryContainer = SunbeamCoralTertiaryContainerLight,
    onTertiaryContainer = SunbeamCoralOnTertiaryContainerLight,
    background = SunbeamBackgroundLight,
    onBackground = SunbeamOnBackgroundLight,
    surface = SunbeamSurfaceLight,
    onSurface = SunbeamOnSurfaceLight,
    surfaceVariant = SunbeamSurfaceVariantLight,
    onSurfaceVariant = SunbeamOnSurfaceVariantLight,
    outline = SunbeamOutlineLight,
    outlineVariant = SunbeamOutlineVariantLight,
)
