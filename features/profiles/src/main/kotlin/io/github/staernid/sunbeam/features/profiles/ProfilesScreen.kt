@file:Suppress(
    "LongParameterList",
    "LongMethod",
)

package io.github.staernid.sunbeam.features.profiles

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import io.github.staernid.sunbeam.features.profiles.component.DeleteProfileConfirmationDialog
import io.github.staernid.sunbeam.features.profiles.component.GamepadTesterView
import io.github.staernid.sunbeam.features.profiles.component.OscPreviewView
import io.github.staernid.sunbeam.features.profiles.component.ProfileCard
import io.github.staernid.sunbeam.features.profiles.component.ProfileFormDialog
import io.github.staernid.sunbeam.features.profiles.model.GamepadDebugState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    uiState: ProfilesUiState,
    onTabSelected: (ProfilesTab) -> Unit,
    onAddProfileClick: () -> Unit,
    onActivateProfile: (ProfileCardId: java.util.UUID?) -> Unit,
    onEditProfileClick: (io.github.staernid.sunbeam.core.data.model.Profile) -> Unit,
    onDuplicateProfileClick: (io.github.staernid.sunbeam.core.data.model.Profile) -> Unit,
    onDeleteProfileClick: (io.github.staernid.sunbeam.core.data.model.Profile) -> Unit,
    onSaveProfile: (
        name: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int,
        stretchMode: Boolean,
        oscEnabled: Boolean,
        oscOpacity: Float,
    ) -> Unit,
    onConfirmDeleteProfile: (java.util.UUID) -> Unit,
    onDismissDialog: () -> Unit,
    onTestRumble: (Float) -> Unit,
    onOscOpacityChange: (Float) -> Unit,
    onOscScaleChange: (Float) -> Unit,
    onFeedbackMessageShown: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onUpdateGamepadState: ((GamepadDebugState) -> GamepadDebugState) -> Unit = {},
    onResetOscLayout: () -> Unit = {},
    onToggleOscEnabled: (Boolean) -> Unit = {},
    onToggleHideOscWhenHasGamepad: (Boolean) -> Unit = {},
    onToggleOnlyShowL3R3: (Boolean) -> Unit = {},
    onToggleShowGuideButton: (Boolean) -> Unit = {},
    onToggleVibrateOsc: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onFeedbackMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Profiles & Controls", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.sunbeamFocusable()) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.selectedTab == ProfilesTab.PROFILES) {
                ExtendedFloatingActionButton(
                    onClick = onAddProfileClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Profile") },
                    modifier = Modifier.sunbeamFocusable(),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ProfilesHeaderTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected,
            )

            Crossfade(
                targetState = uiState.selectedTab,
                modifier = Modifier.fillMaxSize(),
                label = "ProfilesTabCrossfade",
            ) { tab ->
                when (tab) {
                    ProfilesTab.PROFILES -> ProfilesListView(
                        uiState = uiState,
                        onActivateProfile = onActivateProfile,
                        onEditProfileClick = onEditProfileClick,
                        onDuplicateProfileClick = onDuplicateProfileClick,
                        onDeleteProfileClick = onDeleteProfileClick,
                        onAddProfileClick = onAddProfileClick,
                    )
                    ProfilesTab.GAMEPAD_TESTER -> GamepadTesterView(
                        state = uiState.gamepadDebugState,
                        onTestRumble = onTestRumble,
                        onUpdateGamepadState = onUpdateGamepadState,
                    )
                    ProfilesTab.OSC_PREVIEW -> OscPreviewView(
                        opacity = uiState.oscOpacity,
                        scale = uiState.oscScale,
                        onOpacityChange = onOscOpacityChange,
                        onScaleChange = onOscScaleChange,
                        onResetLayout = onResetOscLayout,
                        oscEnabled = uiState.oscEnabled,
                        hideOscWhenHasGamepad = uiState.hideOscWhenHasGamepad,
                        onlyShowL3R3 = uiState.onlyShowL3R3,
                        showGuideButton = uiState.showGuideButton,
                        vibrateOsc = uiState.vibrateOsc,
                        onOscEnabledChange = onToggleOscEnabled,
                        onHideOscWhenHasGamepadChange = onToggleHideOscWhenHasGamepad,
                        onOnlyShowL3R3Change = onToggleOnlyShowL3R3,
                        onShowGuideButtonChange = onToggleShowGuideButton,
                        onVibrateOscChange = onToggleVibrateOsc,
                    )
                }
            }
        }

        ProfileDialogsHost(
            dialogState = uiState.dialogState,
            onSaveProfile = onSaveProfile,
            onConfirmDeleteProfile = onConfirmDeleteProfile,
            onDismissDialog = onDismissDialog,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilesHeaderTabs(
    selectedTab: ProfilesTab,
    onTabSelected: (ProfilesTab) -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Tab(
            selected = selectedTab == ProfilesTab.PROFILES,
            onClick = { onTabSelected(ProfilesTab.PROFILES) },
            icon = { Icon(Icons.Default.Tune, contentDescription = null) },
            text = { Text("Profiles") },
            modifier = Modifier.sunbeamFocusable(),
        )
        Tab(
            selected = selectedTab == ProfilesTab.GAMEPAD_TESTER,
            onClick = { onTabSelected(ProfilesTab.GAMEPAD_TESTER) },
            icon = { Icon(Icons.Default.SportsEsports, contentDescription = null) },
            text = { Text("Gamepad Tester") },
            modifier = Modifier.sunbeamFocusable(),
        )
        Tab(
            selected = selectedTab == ProfilesTab.OSC_PREVIEW,
            onClick = { onTabSelected(ProfilesTab.OSC_PREVIEW) },
            icon = { Icon(Icons.Default.TouchApp, contentDescription = null) },
            text = { Text("On-Screen Controls") },
            modifier = Modifier.sunbeamFocusable(),
        )
    }
}

@Composable
private fun ProfilesListView(
    uiState: ProfilesUiState,
    onActivateProfile: (java.util.UUID?) -> Unit,
    onEditProfileClick: (io.github.staernid.sunbeam.core.data.model.Profile) -> Unit,
    onDuplicateProfileClick: (io.github.staernid.sunbeam.core.data.model.Profile) -> Unit,
    onDeleteProfileClick: (io.github.staernid.sunbeam.core.data.model.Profile) -> Unit,
    onAddProfileClick: () -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.profiles.isEmpty()) {
            EmptyProfilesView(onAddProfileClick = onAddProfileClick)
        } else {
            uiState.profiles.forEach { profile ->
                ProfileCard(
                    profile = profile,
                    isActive = profile.uuid == uiState.activeProfileId,
                    onActivate = { onActivateProfile(profile.uuid) },
                    onEdit = { onEditProfileClick(profile) },
                    onDuplicate = { onDuplicateProfileClick(profile) },
                    onDelete = { onDeleteProfileClick(profile) },
                )
            }
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun EmptyProfilesView(onAddProfileClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Custom Profiles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Create stream profiles to save tailored resolutions, frame rates, and control configurations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(
            onClick = onAddProfileClick,
            modifier = Modifier.sunbeamFocusable(),
        ) {
            Text("Create Profile")
        }
    }
}

@Composable
private fun ProfileDialogsHost(
    dialogState: ProfileDialogState,
    onSaveProfile: (String, Int, Int, Int, Int, Boolean, Boolean, Float) -> Unit,
    onConfirmDeleteProfile: (java.util.UUID) -> Unit,
    onDismissDialog: () -> Unit,
) {
    when (dialogState) {
        is ProfileDialogState.None -> Unit
        is ProfileDialogState.Create -> ProfileFormDialog(
            initialProfile = null,
            onDismiss = onDismissDialog,
            onSave = onSaveProfile,
        )
        is ProfileDialogState.Edit -> ProfileFormDialog(
            initialProfile = dialogState.profile,
            onDismiss = onDismissDialog,
            onSave = onSaveProfile,
        )
        is ProfileDialogState.DeleteConfirmation -> DeleteProfileConfirmationDialog(
            profile = dialogState.profile,
            onConfirm = { onConfirmDeleteProfile(dialogState.profile.uuid) },
            onDismiss = onDismissDialog,
        )
    }
}
