package io.github.staernid.sunbeam.core.preferences.display

import android.annotation.TargetApi
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager

object DeviceDisplayHelper {
    private const val DEFAULT_WIDTH = 1920
    private const val DEFAULT_HEIGHT = 1080
    private const val DEFAULT_REFRESH_RATE = 60f

    fun fromContext(context: Context): DeviceDisplayInfo {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: return DeviceDisplayInfo(DEFAULT_WIDTH, DEFAULT_HEIGHT)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            fromWindowMetrics(wm, context)
        } else {
            fromDefaultDisplay(wm, context)
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun fromWindowMetrics(
        wm: WindowManager,
        context: Context,
    ): DeviceDisplayInfo {
        val metrics = wm.currentWindowMetrics
        val cutoutInsets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.displayCutout(),
        )
        var refreshRate = DEFAULT_REFRESH_RATE
        context.display?.let { display ->
            refreshRate = display.refreshRate
        }

        return DeviceDisplayInfo(
            physicalWidth = metrics.bounds.width(),
            physicalHeight = metrics.bounds.height(),
            safeInsetLeft = cutoutInsets.left,
            safeInsetRight = cutoutInsets.right,
            safeInsetTop = cutoutInsets.top,
            safeInsetBottom = cutoutInsets.bottom,
            refreshRate = refreshRate,
            isTv = isTv(context),
        )
    }

    @Suppress("DEPRECATION")
    private fun fromDefaultDisplay(
        wm: WindowManager,
        context: Context,
    ): DeviceDisplayInfo {
        val display = wm.defaultDisplay
            ?: return DeviceDisplayInfo(DEFAULT_WIDTH, DEFAULT_HEIGHT, isTv = isTv(context))

        val dm = DisplayMetrics()
        display.getRealMetrics(dm)

        var insetLeft = 0
        var insetRight = 0
        var insetTop = 0
        var insetBottom = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            display.cutout?.let { cutout ->
                insetLeft = cutout.safeInsetLeft
                insetRight = cutout.safeInsetRight
                insetTop = cutout.safeInsetTop
                insetBottom = cutout.safeInsetBottom
            }
        }

        return DeviceDisplayInfo(
            physicalWidth = dm.widthPixels,
            physicalHeight = dm.heightPixels,
            safeInsetLeft = insetLeft,
            safeInsetRight = insetRight,
            safeInsetTop = insetTop,
            safeInsetBottom = insetBottom,
            refreshRate = display.refreshRate,
            isTv = isTv(context),
        )
    }

    fun isTv(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true
        }
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            pm.hasSystemFeature("android.hardware.type.television")
    }
}
