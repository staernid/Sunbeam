package io.github.staernid.sunbeam.features.profiles

import android.view.KeyEvent
import io.github.staernid.sunbeam.core.preferences.PreferencesRepository
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.features.profiles.model.GamepadButton
import io.github.staernid.sunbeam.features.profiles.model.GamepadDeviceInfo
import io.github.staernid.sunbeam.features.profiles.model.bitrateKbps
import io.github.staernid.sunbeam.features.profiles.model.createNewProfile
import io.github.staernid.sunbeam.features.profiles.model.fps
import io.github.staernid.sunbeam.features.profiles.model.height
import io.github.staernid.sunbeam.features.profiles.model.oscEnabled
import io.github.staernid.sunbeam.features.profiles.model.oscOpacity
import io.github.staernid.sunbeam.features.profiles.model.stretchMode
import io.github.staernid.sunbeam.features.profiles.model.width
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProfilesViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepo: FakeProfileRepository
    private lateinit var preferencesRepo: TestPreferencesRepository
    private lateinit var viewModel: ProfilesViewModel

    private class TestPreferencesRepository : PreferencesRepository {
        private val state = MutableStateFlow(StreamSettings())
        override val settingsFlow: Flow<StreamSettings> = state.asStateFlow()

        override suspend fun getSettings(): StreamSettings = state.value

        override suspend fun updateSettings(transform: (StreamSettings) -> StreamSettings): StreamSettings {
            state.update(transform)
            return state.value
        }

        override suspend fun setSettings(settings: StreamSettings) {
            state.value = settings
        }
    }

    private val sampleProfile1 = createNewProfile(
        name = "Ultra 4K",
        width = 3840,
        height = 2160,
        fps = 60,
        bitrateKbps = 50_000,
        stretchMode = false,
        oscEnabled = false,
    )
    private val sampleProfile2 = createNewProfile(
        name = "Smooth 120Hz",
        width = 1920,
        height = 1080,
        fps = 120,
        bitrateKbps = 30_000,
        stretchMode = true,
        oscEnabled = true,
        oscOpacity = 0.5f,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        profileRepo = FakeProfileRepository(
            initialProfiles = listOf(sampleProfile1, sampleProfile2),
            initialActiveId = sampleProfile1.uuid,
        )
        preferencesRepo = TestPreferencesRepository()
        viewModel = ProfilesViewModel(profileRepo, preferencesRepo)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateLoadsProfiles() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.profiles.size)
        assertEquals(sampleProfile1.uuid, state.activeProfileId)
        assertEquals(ProfilesTab.PROFILES, state.selectedTab)
    }

    @Test
    fun testSelectTab() {
        viewModel.selectTab(ProfilesTab.GAMEPAD_TESTER)
        assertEquals(ProfilesTab.GAMEPAD_TESTER, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(ProfilesTab.OSC_PREVIEW)
        assertEquals(ProfilesTab.OSC_PREVIEW, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun testCreateProfile() = runTest(testDispatcher) {
        viewModel.createProfile(
            name = "Portable 720p",
            width = 1280,
            height = 720,
            fps = 60,
            bitrateKbps = 10_000,
            stretchMode = false,
            oscEnabled = true,
            oscOpacity = 0.8f,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.profiles.size)
        val created = state.profiles.firstOrNull { it.name == "Portable 720p" }
        assertNotNull(created)
        assertEquals(1280, created!!.width)
        assertEquals(720, created.height)
        assertEquals(60, created.fps)
        assertEquals(10_000, created.bitrateKbps)
        assertFalse(created.stretchMode)
        assertTrue(created.oscEnabled)
        assertEquals(0.8f, created.oscOpacity)
        assertEquals(ProfileDialogState.None, state.dialogState)
        assertEquals("Profile 'Portable 720p' created", state.userFeedbackMessage)
    }

    @Test
    fun testUpdateProfile() = runTest(testDispatcher) {
        viewModel.updateProfile(
            uuid = sampleProfile1.uuid,
            name = "Ultra 4K HDR",
            width = 3840,
            height = 2160,
            fps = 90,
            bitrateKbps = 80_000,
            stretchMode = true,
            oscEnabled = false,
            oscOpacity = 0.7f,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val updated = state.profiles.first { it.uuid == sampleProfile1.uuid }
        assertEquals("Ultra 4K HDR", updated.name)
        assertEquals(90, updated.fps)
        assertEquals(80_000, updated.bitrateKbps)
        assertTrue(updated.stretchMode)
        assertEquals("Profile 'Ultra 4K HDR' updated", state.userFeedbackMessage)
    }

    @Test
    fun testDeleteProfile() = runTest(testDispatcher) {
        viewModel.deleteProfile(sampleProfile2.uuid)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.profiles.size)
        assertNull(state.profiles.firstOrNull { it.uuid == sampleProfile2.uuid })
        assertEquals("Profile 'Smooth 120Hz' deleted", state.userFeedbackMessage)
    }

    @Test
    fun testSetActiveProfile() = runTest(testDispatcher) {
        viewModel.setActiveProfile(sampleProfile2.uuid)
        advanceUntilIdle()
        assertEquals(sampleProfile2.uuid, viewModel.uiState.value.activeProfileId)

        viewModel.setActiveProfile(null)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.activeProfileId)
    }

    @Test
    fun testDuplicateProfile() = runTest(testDispatcher) {
        viewModel.duplicateProfile(sampleProfile1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.profiles.size)
        val copy = state.profiles.firstOrNull { it.name == "Ultra 4K (Copy)" }
        assertNotNull(copy)
        assertEquals(sampleProfile1.width, copy!!.width)
        assertEquals(sampleProfile1.height, copy.height)
        assertEquals(sampleProfile1.bitrateKbps, copy.bitrateKbps)
    }

    @Test
    fun testDialogStates() {
        viewModel.showCreateDialog()
        assertEquals(ProfileDialogState.Create, viewModel.uiState.value.dialogState)

        viewModel.showEditDialog(sampleProfile1)
        assertEquals(ProfileDialogState.Edit(sampleProfile1), viewModel.uiState.value.dialogState)

        viewModel.showDeleteDialog(sampleProfile2)
        assertEquals(ProfileDialogState.DeleteConfirmation(sampleProfile2), viewModel.uiState.value.dialogState)

        viewModel.dismissDialog()
        assertEquals(ProfileDialogState.None, viewModel.uiState.value.dialogState)
    }

    @Test
    fun testGamepadAxesAndButtons() {
        viewModel.updateAxes(
            leftStickX = 0.5f,
            leftStickY = -0.75f,
            rightStickX = -0.2f,
            rightStickY = 0.8f,
            leftTrigger = 0.4f,
            rightTrigger = 1.0f,
        )

        val debugState = viewModel.uiState.value.gamepadDebugState
        assertEquals(0.5f, debugState.leftStickX, 0.001f)
        assertEquals(-0.75f, debugState.leftStickY, 0.001f)
        assertEquals(-0.2f, debugState.rightStickX, 0.001f)
        assertEquals(0.8f, debugState.rightStickY, 0.001f)
        assertEquals(0.4f, debugState.leftTrigger, 0.001f)
        assertEquals(1.0f, debugState.rightTrigger, 0.001f)

        viewModel.setButtonPressed(GamepadButton.A, true)
        viewModel.setButtonPressed(GamepadButton.DPAD_UP, true)
        assertTrue(viewModel.uiState.value.gamepadDebugState.pressedButtons.contains(GamepadButton.A))
        assertTrue(viewModel.uiState.value.gamepadDebugState.pressedButtons.contains(GamepadButton.DPAD_UP))

        viewModel.setButtonPressed(GamepadButton.A, false)
        assertFalse(viewModel.uiState.value.gamepadDebugState.pressedButtons.contains(GamepadButton.A))
        assertTrue(viewModel.uiState.value.gamepadDebugState.pressedButtons.contains(GamepadButton.DPAD_UP))
    }

    @Test
    fun testGamepadSensorsAndRumble() {
        val device = GamepadDeviceInfo(1, "Xbox Wireless Controller", 0x045e, 0x02fd, hasVibrator = true)
        viewModel.setConnectedDevices(listOf(device))
        assertEquals(1, viewModel.uiState.value.gamepadDebugState.devices.size)
        assertEquals(1, viewModel.uiState.value.gamepadDebugState.selectedDeviceId)

        viewModel.updateSensors(
            gyroX = 0.12f,
            gyroY = -0.45f,
            gyroZ = 0.88f,
            accelX = 0.0f,
            accelY = 9.8f,
            accelZ = 0.1f,
        )
        val debugState = viewModel.uiState.value.gamepadDebugState
        assertEquals(0.12f, debugState.gyroX, 0.001f)
        assertEquals(9.8f, debugState.accelY, 0.001f)

        viewModel.setRumbleStrength(0.9f)
        assertEquals(0.9f, viewModel.uiState.value.gamepadDebugState.rumbleStrength, 0.001f)

        viewModel.setRumbleActive(true)
        assertTrue(viewModel.uiState.value.gamepadDebugState.isRumbling)
    }

    @Test
    fun testHandleKeyEvent() {
        val keyDownEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_X)
        val handled = viewModel.handleKeyEvent(keyDownEvent)
        assertTrue(handled)
        assertTrue(viewModel.uiState.value.gamepadDebugState.pressedButtons.contains(GamepadButton.X))

        val keyUpEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_X)
        viewModel.handleKeyEvent(keyUpEvent)
        assertFalse(viewModel.uiState.value.gamepadDebugState.pressedButtons.contains(GamepadButton.X))
    }

    @Test
    fun testOscOpacityAndScalePersistsToPreferences() = runTest(testDispatcher) {
        viewModel.setOscOpacity(0.45f)
        advanceUntilIdle()
        assertEquals(0.45f, viewModel.uiState.value.oscOpacity, 0.001f)
        assertEquals(45, preferencesRepo.getSettings().oscOpacity)

        viewModel.setOscScale(1.4f)
        assertEquals(1.4f, viewModel.uiState.value.oscScale, 0.001f)

        viewModel.resetOscLayout()
        advanceUntilIdle()
        assertEquals(0.9f, viewModel.uiState.value.oscOpacity, 0.001f)
        assertEquals(1.0f, viewModel.uiState.value.oscScale, 0.001f)
        assertEquals(90, preferencesRepo.getSettings().oscOpacity)
        assertEquals(
            "On-screen controls layout reset to default",
            viewModel.uiState.value.userFeedbackMessage,
        )
    }

    @Test
    fun testClearFeedbackMessage() = runTest(testDispatcher) {
        viewModel.setActiveProfile(sampleProfile2.uuid)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.userFeedbackMessage)

        viewModel.clearFeedbackMessage()
        assertNull(viewModel.uiState.value.userFeedbackMessage)
    }
}
