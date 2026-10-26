package io.github.staernid.sunbeam.stream.overlay

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import io.github.staernid.sunbeam.R

/**
 * Centralized dispatcher for transient in-stream user notifications and toasts.
 */
object StreamToastHelper {
    @JvmStatic
    fun showToast(
        context: Context,
        message: CharSequence,
        duration: Int = Toast.LENGTH_SHORT,
    ) {
        Toast.makeText(context, message, duration).show()
    }

    @JvmStatic
    fun showToast(
        context: Context,
        @StringRes stringResId: Int,
        duration: Int = Toast.LENGTH_SHORT,
    ) {
        Toast.makeText(context, stringResId, duration).show()
    }

    @JvmStatic
    fun showPanZoomToast(
        context: Context,
        enabled: Boolean,
    ) {
        val resId = if (enabled) R.string.pan_zoom_mode_enabled else R.string.pan_zoom_mode_disabled
        showToast(context, resId, Toast.LENGTH_SHORT)
    }

    @JvmStatic
    fun showLatencyToast(
        context: Context,
        message: String,
    ) {
        showToast(context, message, Toast.LENGTH_LONG)
    }

    @JvmStatic
    fun showQuitSuccessToast(
        context: Context,
        appName: String,
    ) {
        val text = "${context.getString(R.string.applist_quit_success)} $appName"
        showToast(context, text, Toast.LENGTH_LONG)
    }
}
