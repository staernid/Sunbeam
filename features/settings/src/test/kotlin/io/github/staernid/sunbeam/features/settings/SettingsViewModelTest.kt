package io.github.staernid.sunbeam.features.settings

import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.FramePacingMode
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferencesRepo: FakePreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        preferencesRepo = FakePreferencesRepository()
        viewModel = SettingsViewModel(
            preferencesRepository = preferencesRepo,
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads settings from repository`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1280, state.settings.width)
        assertEquals(720, state.settings.height)
        assertEquals(60, state.settings.fps)
    }

    @Test
    fun `updateResolution modifies width and height and dismisses dialog`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.openDialog(SettingsDialogState.ResolutionPicker)
        assertEquals(SettingsDialogState.ResolutionPicker, viewModel.uiState.value.dialogState)

        viewModel.updateResolution(1920, 1080)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1920, state.settings.width)
        assertEquals(1080, state.settings.height)
        assertEquals(SettingsDialogState.None, state.dialogState)
    }

    @Test
    fun `updateFps modifies fps and dismisses dialog`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.openDialog(SettingsDialogState.FpsPicker)
        viewModel.updateFps(120)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(120, state.settings.fps)
        assertEquals(SettingsDialogState.None, state.dialogState)
    }

    @Test
    fun `updateBitrateMbps scales to kbps and updates settings`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateBitrateMbps(25)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(25000, state.settings.bitrateKbps)
        assertEquals(25, state.bitrateMbps)
    }

    @Test
    fun `updateVideoScaleMode updates scale mode and dismisses dialog`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.openDialog(SettingsDialogState.VideoScalePicker)
        viewModel.updateVideoScaleMode(VideoScaleMode.STRETCH)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(VideoScaleMode.STRETCH, state.settings.videoScaleMode)
        assertEquals(SettingsDialogState.None, state.dialogState)
    }

    @Test
    fun `updateVideoFormat updates format and dismisses dialog`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.openDialog(SettingsDialogState.VideoFormatPicker)
        viewModel.updateVideoFormat(VideoFormatOption.FORCE_AV1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(VideoFormatOption.FORCE_AV1, state.settings.videoFormat)
        assertEquals(SettingsDialogState.None, state.dialogState)
    }

    @Test
    fun `updateAudioConfig updates audio configuration and dismisses dialog`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.openDialog(SettingsDialogState.AudioConfigPicker)
        viewModel.updateAudioConfig(AudioConfiguration.SURROUND_51)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AudioConfiguration.SURROUND_51, state.settings.audioConfig)
        assertEquals(SettingsDialogState.None, state.dialogState)
    }

    @Test
    fun `updateFramePacing updates frame pacing and dismisses dialog`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.openDialog(SettingsDialogState.FramePacingPicker)
        viewModel.updateFramePacing(FramePacingMode.BALANCED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(FramePacingMode.BALANCED, state.settings.framePacing)
        assertEquals(SettingsDialogState.None, state.dialogState)
    }

    @Test
    fun `updateDeadzone and updateOscOpacity update respective values`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateDeadzone(15)
        viewModel.updateOscOpacity(75)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(15, state.settings.deadzonePercentage)
        assertEquals(75, state.settings.oscOpacity)
    }

    @Test
    fun `resetToDefaults resets settings and posts user message`() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Modify settings first
        viewModel.updateResolution(3840, 2160)
        viewModel.updateFps(120)
        advanceUntilIdle()

        viewModel.openDialog(SettingsDialogState.ResetConfirmation)
        viewModel.resetToDefaults()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StreamSettings().width, state.settings.width)
        assertEquals(StreamSettings().fps, state.settings.fps)
        assertEquals(SettingsDialogState.None, state.dialogState)
        assertNotNull(state.userMessage)
    }
}
