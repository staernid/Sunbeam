@file:Suppress(
    "LongParameterList",
    "LongMethod",
)

package io.github.staernid.sunbeam.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamCenteredLoadingScreen
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamTopAppBar
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.FramePacingMode
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode
import io.github.staernid.sunbeam.features.settings.component.AdvancedControllerSettingsSection
import io.github.staernid.sunbeam.features.settings.component.AdvancedDisplaySettingsSection
import io.github.staernid.sunbeam.features.settings.component.AppearanceSettingsSection
import io.github.staernid.sunbeam.features.settings.component.AudioSettingsSection
import io.github.staernid.sunbeam.features.settings.component.InputSettingsSection
import io.github.staernid.sunbeam.features.settings.component.KeyboardSettingsSection
import io.github.staernid.sunbeam.features.settings.component.OscSettingsSection
import io.github.staernid.sunbeam.features.settings.component.PerfSettingsSection
import io.github.staernid.sunbeam.features.settings.component.QuickPresetSection
import io.github.staernid.sunbeam.features.settings.component.SettingsDialogsHost
import io.github.staernid.sunbeam.features.settings.component.TouchMouseSettingsSection
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
    var isSearchActive by rememberSaveable(uiState.isSearchActive) {
        mutableStateOf(uiState.isSearchActive)
    }
    var searchQuery by rememberSaveable(uiState.searchQuery) {
        mutableStateOf(uiState.searchQuery)
    }

    uiState.userMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            onUserMessageDismissed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchToggle = {
                    if (isSearchActive) {
                        isSearchActive = false
                        searchQuery = ""
                    } else {
                        isSearchActive = true
                    }
                },
                onSearchQueryChange = { searchQuery = it },
                onBackClick = onBackClick,
                onResetClick = { onOpenDialog(SettingsDialogState.ResetConfirmation) },
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
                if (searchQuery.isBlank()) {
                    QuickPresetSection(
                        activePreset = uiState.activePreset,
                        onPresetSelected = { preset ->
                            if (preset != StreamQualityPreset.CUSTOM) {
                                onUpdateSetting(preset::applyTo)
                            }
                        },
                    )
                }

                AppearanceSettingsSection(
                    settings = uiState.settings,
                    onUpdate = onUpdateSetting,
                    searchQuery = searchQuery,
                )

                VideoSettingsSection(
                    settings = uiState.settings,
                    bitrateMbps = uiState.bitrateMbps,
                    onOpenDialog = onOpenDialog,
                    onBitrateChange = onBitrateChange,
                    onUpdate = onUpdateSetting,
                    searchQuery = searchQuery,
                )

                AdvancedDisplaySettingsSection(
                    settings = uiState.settings,
                    onUpdate = onUpdateSetting,
                    searchQuery = searchQuery,
                )

                AudioSettingsSection(
                    settings = uiState.settings,
                    onOpenDialog = onOpenDialog,
                    onUpdate = onUpdateSetting,
                    searchQuery = searchQuery,
                )

                InputSettingsSection(
                    settings = uiState.settings,
                    onDeadzoneChange = onDeadzoneChange,
                    onUpdate = onUpdateSetting,
                    searchQuery = searchQuery,
                )

                AdvancedControllerSettingsSection(
                    settings = uiState.settings,
                    onUpdate = onUpdateSetting,
                    searchQuery = searchQuery,
                )

                TouchMouseSettingsSection(
                    settings = uiState.settings,
                    onOpenDialog = onOpenDialog,
                    onUpdate = onUpdateSetting,
                    searchQuery = searchQuery,
                )

                KeyboardSettingsSection(
                    settings = uiState.settings,
                    onUpdate = onUpdateSetting,
                    searchQuery = searchQuery,
                )

                OscSettingsSection(
                    settings = uiState.settings,
                    onOpacityChange = onOscOpacityChange,
                    onUpdate = onUpdateSetting,
                    searchQuery = searchQuery,
                )

                PerfSettingsSection(
                    settings = uiState.settings,
                    onUpdate = onUpdateSetting,
                    searchQuery = searchQuery,
                )

                Spacer(modifier = Modifier.height(16.dp))
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
        onAnalogStickScrollingSelect = { scrolling ->
            onUpdateSetting { it.copy(analogStickScrolling = scrolling) }
            onDismissDialog()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onResetClick: () -> Unit,
) {
    if (isSearchActive) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search settings...") },
            leadingIcon = {
                IconButton(onClick = onSearchToggle, modifier = Modifier.sunbeamFocusable()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close search",
                    )
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.sunbeamFocusable()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    } else {
        SunbeamTopAppBar(
            title = "Settings",
            navigationIcon = {
                IconButton(onClick = onBackClick, modifier = Modifier.sunbeamFocusable()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                IconButton(onClick = onSearchToggle, modifier = Modifier.sunbeamFocusable()) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                    )
                }
                IconButton(onClick = onResetClick, modifier = Modifier.sunbeamFocusable()) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Reset Settings",
                    )
                }
            },
        )
    }
}
