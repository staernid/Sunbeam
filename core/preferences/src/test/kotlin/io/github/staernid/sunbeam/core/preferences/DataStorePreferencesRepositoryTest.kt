@file:Suppress("MagicNumber")

package io.github.staernid.sunbeam.core.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStorePreferencesRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: DataStorePreferencesRepository

    @Before
    fun setUp() {
        val testFile = tempFolder.newFile("test_settings.preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { testFile },
            )
        repository = DataStorePreferencesRepository(dataStore)
    }

    @Test
    fun defaultSettings_matchExpectedDefaults() =
        testScope.runTest {
            val settings = repository.getSettings()

            assertEquals(1280, settings.width)
            assertEquals(720, settings.height)
            assertEquals(60, settings.fps)
            assertEquals(10000, settings.bitrateKbps)
            assertEquals(VideoScaleMode.FIT, settings.videoScaleMode)
            assertFalse(settings.dynamicColor)
        }

    @Test
    fun updateSettings_mutatesAndEmitsNewValues() =
        testScope.runTest {
            val updated =
                repository.updateSettings { current ->
                    current.copy(
                        width = 1920,
                        height = 1080,
                        fps = 120,
                        bitrateKbps = 25000,
                        enableHdr = true,
                        videoFormat = VideoFormatOption.FORCE_HEVC,
                    )
                }

            assertEquals(1920, updated.width)
            assertEquals(1080, updated.height)
            assertEquals(120, updated.fps)
            assertEquals(25000, updated.bitrateKbps)
            assertTrue(updated.enableHdr)
            assertEquals(VideoFormatOption.FORCE_HEVC, updated.videoFormat)

            val flowValue = repository.settingsFlow.first()
            assertEquals(1920, flowValue.width)
            assertEquals(25000, flowValue.bitrateKbps)
        }

    @Test
    fun setSettings_overwritesEntireConfiguration() =
        testScope.runTest {
            val custom =
                StreamSettings(
                    width = 3840,
                    height = 2160,
                    fps = 60,
                    bitrateKbps = 60000,
                    audioConfig = AudioConfiguration.SURROUND_51,
                    enablePerfOverlay = true,
                )

            repository.setSettings(custom)

            val retrieved = repository.getSettings()
            assertEquals(3840, retrieved.width)
            assertEquals(2160, retrieved.height)
            assertEquals(60000, retrieved.bitrateKbps)
            assertEquals(AudioConfiguration.SURROUND_51, retrieved.audioConfig)
            assertTrue(retrieved.enablePerfOverlay)
        }
}
