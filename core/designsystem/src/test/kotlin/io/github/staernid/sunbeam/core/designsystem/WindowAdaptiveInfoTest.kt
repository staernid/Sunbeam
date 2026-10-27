package io.github.staernid.sunbeam.core.designsystem

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.designsystem.adaptive.SunbeamNavigationType
import io.github.staernid.sunbeam.core.designsystem.adaptive.calculateSunbeamAdaptiveInfo
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class WindowAdaptiveInfoTest {
    @Test
    fun `compact width yields bottom bar and 2 grid columns`() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))
        assertEquals(WindowWidthSizeClass.Compact, windowSizeClass.widthSizeClass)
        assertEquals(WindowHeightSizeClass.Medium, windowSizeClass.heightSizeClass)

        val adaptiveInfo = calculateSunbeamAdaptiveInfo(windowSizeClass, isTv = false)
        assertEquals(SunbeamNavigationType.BottomBar, adaptiveInfo.navigationType)
        assertEquals(2, adaptiveInfo.preferredGridColumns)
    }

    @Test
    fun `medium width yields navigation rail and 3 grid columns`() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(700.dp, 900.dp))
        assertEquals(WindowWidthSizeClass.Medium, windowSizeClass.widthSizeClass)

        val adaptiveInfo = calculateSunbeamAdaptiveInfo(windowSizeClass, isTv = false)
        assertEquals(SunbeamNavigationType.NavigationRail, adaptiveInfo.navigationType)
        assertEquals(3, adaptiveInfo.preferredGridColumns)
    }

    @Test
    fun `expanded width yields permanent navigation drawer and 4 grid columns`() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1000.dp, 800.dp))
        assertEquals(WindowWidthSizeClass.Expanded, windowSizeClass.widthSizeClass)

        val adaptiveInfo = calculateSunbeamAdaptiveInfo(windowSizeClass, isTv = false)
        assertEquals(SunbeamNavigationType.PermanentNavigationDrawer, adaptiveInfo.navigationType)
        assertEquals(4, adaptiveInfo.preferredGridColumns)
    }

    @Test
    fun `compact height landscape phone yields navigation rail`() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(500.dp, 350.dp))
        assertEquals(WindowWidthSizeClass.Compact, windowSizeClass.widthSizeClass)
        assertEquals(WindowHeightSizeClass.Compact, windowSizeClass.heightSizeClass)

        val adaptiveInfo = calculateSunbeamAdaptiveInfo(windowSizeClass, isTv = false)
        assertEquals(SunbeamNavigationType.NavigationRail, adaptiveInfo.navigationType)
    }

    @Test
    fun `tv device forces permanent drawer and 5 columns`() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1920.dp, 1080.dp))

        val adaptiveInfo = calculateSunbeamAdaptiveInfo(windowSizeClass, isTv = true)
        assertEquals(SunbeamNavigationType.PermanentNavigationDrawer, adaptiveInfo.navigationType)
        assertEquals(5, adaptiveInfo.preferredGridColumns)
    }
}
