@file:Suppress(
    "LongParameterList",
    "MagicNumber",
)

package io.github.staernid.sunbeam.stream.overlay

import android.app.AlertDialog
import android.content.Context
import io.github.staernid.sunbeam.Game
import io.github.staernid.sunbeam.R
import io.github.staernid.sunbeam.binding.input.GameInputDevice
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import io.github.staernid.sunbeam.profiles.ProfilesManager
import io.github.staernid.sunbeam.utils.ExternalDisplayControlActivity
import io.github.staernid.sunbeam.utils.MouseModeOption

fun interface MouseModeConsumer {
    fun onMouseModeSelected(mode: Int)
}

/**
 * Coordinates game menu presentations, dialogs (mouse mode selection, session quit confirmation),
 * and routing between the primary screen and external displays.
 */
class StreamMenuCoordinator(
    private val isOnExternalDisplay: () -> Boolean,
    private val getMenuCallbacks: () -> Game.GameMenuCallbacks?,
) {
    fun showGameMenu(device: GameInputDevice? = null) {
        if (isOnExternalDisplay()) {
            ExternalDisplayControlActivity.toggleGameMenu()
        } else {
            getMenuCallbacks()?.showMenu(device)
        }
    }

    fun hideGameMenu() {
        getMenuCallbacks()?.hideMenu()
    }

    fun isMenuOpen(): Boolean = getMenuCallbacks()?.isMenuOpen == true

    fun showQuitConfirmDialog(
        context: Context,
        onConfirmQuit: Runnable,
    ) {
        val targetContext =
            if (isOnExternalDisplay() && ExternalDisplayControlActivity.instance != null) {
                ExternalDisplayControlActivity.instance
            } else {
                context
            }

        AlertDialog.Builder(targetContext)
            .setTitle(R.string.game_dialog_title_quit_confirm)
            .setMessage(R.string.game_dialog_message_quit_confirm)
            .setPositiveButton(targetContext.getString(R.string.yes)) { _, _ ->
                onConfirmQuit.run()
            }
            .setNegativeButton(targetContext.getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    fun showSelectMouseModeDialog(
        context: Context,
        prefConfig: PreferenceConfiguration,
        onApplyMouseMode: MouseModeConsumer,
        onToggleLocalCursor: Runnable,
    ) {
        val allModes = context.resources.getStringArray(R.array.mouse_mode_names)
        val allowedLabels =
            setOf(
                context.getString(R.string.mouse_mode_track_pad_natural),
                context.getString(R.string.mouse_mode_track_pad_gaming),
                context.getString(R.string.mouse_mode_disabled),
            )

        val options = mutableListOf<MouseModeOption>()
        for (i in allModes.indices) {
            val label = allModes[i]
            val isAllowed = !isOnExternalDisplay() || allowedLabels.contains(label)
            if (isAllowed) {
                options.add(MouseModeOption(i, label))
            }
        }
        options.add(MouseModeOption(-1, context.getString(R.string.toggle_local_mouse_cursor)))

        val labels = Array(options.size) { options[it].label }

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.game_menu_select_mouse_mode))
            .setItems(labels) { dialog, which ->
                dialog.dismiss()
                val selected = options[which]
                if (selected.index == -1) {
                    onToggleLocalCursor.run()
                } else {
                    onApplyMouseMode.onMouseModeSelected(selected.index)
                    if (prefConfig.rememberMouseMode) {
                        ProfilesManager.getInstance().getOverlayingSharedPreferences(context)
                            .edit()
                            .putString("mouse_mode_list", selected.index.toString())
                            .apply()
                    }
                }
            }
            .create()
            .show()
    }
}
