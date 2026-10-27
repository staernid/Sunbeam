package io.github.staernid.sunbeam.features.stream.component

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Surface presentation layer that wraps [SurfaceView] inside [AndroidView].
 *
 * Surface lifecycle events are forwarded to the renderer without per-frame object allocations.
 */
@Composable
fun StreamSurface(
    onSurfaceCreated: (SurfaceHolder) -> Unit,
    onSurfaceChanged: (holder: SurfaceHolder, format: Int, width: Int, height: Int) -> Unit,
    onSurfaceDestroyed: (SurfaceHolder) -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float? = null,
    onSurfaceViewCreated: ((SurfaceView) -> Unit)? = null,
) {
    val context = LocalContext.current
    val surfaceCallback = remember(onSurfaceCreated, onSurfaceChanged, onSurfaceDestroyed) {
        object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                onSurfaceCreated(holder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
                onSurfaceChanged(holder, format, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                onSurfaceDestroyed(holder)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val surfaceModifier = if (aspectRatio != null && aspectRatio > 0f) {
            Modifier.aspectRatio(aspectRatio)
        } else {
            Modifier.fillMaxSize()
        }

        AndroidView(
            modifier = surfaceModifier,
            factory = {
                SurfaceView(context).apply {
                    holder.addCallback(surfaceCallback)
                    onSurfaceViewCreated?.invoke(this)
                }
            },
            update = {
                // SurfaceView is managed by hardware composer; no per-recomposition allocations
            },
        )
    }

    DisposableEffect(surfaceCallback) {
        onDispose {
            // Cleanup on dispose
        }
    }
}
