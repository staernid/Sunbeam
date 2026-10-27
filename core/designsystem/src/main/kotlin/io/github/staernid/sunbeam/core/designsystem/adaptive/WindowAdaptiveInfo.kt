package io.github.staernid.sunbeam.core.designsystem.adaptive

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

enum class SunbeamNavigationType {
    BottomBar,
    NavigationRail,
    PermanentNavigationDrawer,
}

data class SunbeamAdaptiveInfo(
    val windowSizeClass: WindowSizeClass,
    val isTv: Boolean,
    val navigationType: SunbeamNavigationType,
    val preferredGridColumns: Int,
)

private const val TV_GRID_COLUMNS = 5
private const val EXPANDED_GRID_COLUMNS = 4
private const val MEDIUM_GRID_COLUMNS = 3
private const val COMPACT_GRID_COLUMNS = 2

fun calculateSunbeamAdaptiveInfo(
    windowSizeClass: WindowSizeClass,
    isTv: Boolean,
): SunbeamAdaptiveInfo {
    val navType = when {
        isTv -> SunbeamNavigationType.PermanentNavigationDrawer
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> {
            SunbeamNavigationType.PermanentNavigationDrawer
        }
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> SunbeamNavigationType.NavigationRail
        windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> SunbeamNavigationType.NavigationRail
        else -> SunbeamNavigationType.BottomBar
    }

    val columns = when {
        isTv -> TV_GRID_COLUMNS
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> EXPANDED_GRID_COLUMNS
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> MEDIUM_GRID_COLUMNS
        else -> COMPACT_GRID_COLUMNS
    }

    return SunbeamAdaptiveInfo(
        windowSizeClass = windowSizeClass,
        isTv = isTv,
        navigationType = navType,
        preferredGridColumns = columns,
    )
}

@Composable
fun rememberSunbeamAdaptiveInfo(
    windowSizeClass: WindowSizeClass,
    isTv: Boolean = rememberIsTv(),
): SunbeamAdaptiveInfo {
    return remember(windowSizeClass, isTv) {
        calculateSunbeamAdaptiveInfo(windowSizeClass, isTv)
    }
}
