@file:Suppress("MagicNumber")

package io.github.staernid.sunbeam.core.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SharedPreferencesMigrationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun migration_importsExistingSharedPreferences() =
        testScope.runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val prefsName = "test_legacy_prefs"
            val sharedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

            sharedPrefs.edit()
                .putString("list_resolution", "1920x1080")
                .putString("list_fps", "90")
                .putInt("seekbar_bitrate_kbps", 30000)
                .putString("list_video_scale_mode", "stretch")
                .putString("video_format", "hevc")
                .putBoolean("checkbox_enable_hdr", true)
                .putBoolean("checkbox_show_onscreen_controls", true)
                .putInt("seekbar_deadzone", 15)
                .commit()

            val repository =
                DataStorePreferencesRepository.create(
                    context = context,
                    dataStoreName = "test_migration_datastore",
                    legacySharedPrefsName = prefsName,
                    scope = testScope,
                )

            val settings = repository.getSettings()

            assertEquals(1920, settings.width)
            assertEquals(1080, settings.height)
            assertEquals(90, settings.fps)
            assertEquals(30000, settings.bitrateKbps)
            assertEquals(VideoScaleMode.STRETCH, settings.videoScaleMode)
            assertEquals(VideoFormatOption.FORCE_HEVC, settings.videoFormat)
            assertTrue(settings.enableHdr)
            assertTrue(settings.onscreenController)
            assertEquals(15, settings.deadzonePercentage)
        }

    @Test
    fun updateSettings_syncsToSharedPreferences() =
        testScope.runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val prefsName = "test_sync_prefs"
            val sharedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

            val repository =
                DataStorePreferencesRepository.create(
                    context = context,
                    dataStoreName = "test_sync_datastore",
                    legacySharedPrefsName = prefsName,
                    scope = testScope,
                )

            repository.updateSettings { current ->
                current.copy(
                    width = 2560,
                    height = 1440,
                    fps = 120,
                    bitrateKbps = 45000,
                )
            }

            assertEquals("2560x1440", sharedPrefs.getString("list_resolution", null))
            assertEquals("120", sharedPrefs.getString("list_fps", null))
            assertEquals(45000, sharedPrefs.getInt("seekbar_bitrate_kbps", 0))
        }
}
