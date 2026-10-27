@file:Suppress(
    "LongParameterList",
    "LongMethod",
)

package io.github.staernid.sunbeam.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamCenteredLoadingScreen
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamTopAppBar
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.FramePacingMode
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode
import io.github.staernid.sunbeam.features.settings.component.AudioSettingsSection
import io.github.staernid.sunbeam.features.settings.component.InputSettingsSection
import io.github.staernid.sunbeam.features.settings.component.OscSettingsSection
import io.github.staernid.sunbeam.features.settings.component.PerfSettingsSection
import io.github.staernid.sunbeam.features.settings.component.SettingsDialogsHost
import io.github.staernid.sunbeam.features.settings.component.VideoSettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBackClick: () -> Unit,
    onOpenDialog: (SettingsDialogState) -> Unit,
    onDismissDialog: () -> Unit,
    onBitrateChange: (Int) -> Unit,
    onDeadzoneChange: (Int) -> Unit,
    onOscOpacityChange: (Int) -> Unit,
    onResolutionSelect: (Int, Int) -> Unit,
    onFpsSelect: (Int) -> Unit,
    onVideoScaleSelect: (VideoScaleMode) -> Unit,
    onVideoFormatSelect: (VideoFormatOption) -> Unit,
    onAudioConfigSelect: (AudioConfiguration) -> Unit,
    onFramePacingSelect: (FramePacingMode) -> Unit,
    onUpdateSetting: ((StreamSettings) -> StreamSettings) -> Unit,
    onConfirmReset: () -> Unit,
    onUserMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.userMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            onUserMessageDismissed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SunbeamTopAppBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenDialog(SettingsDialogState.ResetConfirmation) }) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Reset Settings",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            SunbeamCenteredLoadingScreen(modifier = Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                VideoSettingsSection(
                    settings = uiState.settings,
                    bitrateMbps = uiState.bitrateMbps,
                    onOpenDialog = onOpenDialog,
                    onBitrateChange = onBitrateChange,
                    onUpdate = onUpdateSetting,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AudioSettingsSection(
                    settings = uiState.settings,
                    onOpenDialog = onOpenDialog,
                    onUpdate = onUpdateSetting,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InputSettingsSection(
                    settings = uiState.settings,
                    onDeadzoneChange = onDeadzoneChange,
                    onUpdate = onUpdateSetting,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                OscSettingsSection(
                    settings = uiState.settings,
                    onOpacityChange = onOscOpacityChange,
                    onUpdate = onUpdateSetting,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PerfSettingsSection(
                    settings = uiState.settings,
                    onUpdate = onUpdateSetting,
                )
            }
        }
    }

    SettingsDialogsHost(
        uiState = uiState,
        onDismissDialog = onDismissDialog,
        onResolutionSelect = onResolutionSelect,
        onFpsSelect = onFpsSelect,
        onVideoScaleSelect = onVideoScaleSelect,
        onVideoFormatSelect = onVideoFormatSelect,
        onAudioConfigSelect = onAudioConfigSelect,
        onFramePacingSelect = onFramePacingSelect,
        onConfirmReset = onConfirmReset,
    )
}
