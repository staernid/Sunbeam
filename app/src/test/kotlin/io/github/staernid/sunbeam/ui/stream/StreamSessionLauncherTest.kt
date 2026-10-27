@file:Suppress("EmptyFunctionBlock")

package io.github.staernid.sunbeam.ui.stream

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.Game
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.data.model.Profile
import io.github.staernid.sunbeam.core.data.repository.AppRepository
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import io.github.staernid.sunbeam.core.data.repository.ProfileRepository
import io.github.staernid.sunbeam.core.preferences.PreferencesRepository
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.stream.model.AudioChannelConfiguration
import io.github.staernid.sunbeam.core.stream.model.VideoFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class StreamSessionLauncherTest {
    private lateinit var context: Context

    private val testHost =
        Host(
            uuid = "test-host-uuid",
            name = "Gaming Rig",
            localAddress = HostAddress(address = "192.168.1.50", port = 47989),
        )

    private val testApp =
        HostApp(
            id = 101,
            title = "Steam Big Picture",
            isHdrSupported = true,
        )

    private val fakeHostRepo =
        object : HostRepository {
            override val hostsFlow: Flow<List<Host>> = flowOf(listOf(testHost))

            override suspend fun getAllHosts(): List<Host> = listOf(testHost)

            override suspend fun getHostByUuid(uuid: String): Host? = if (uuid == testHost.uuid) testHost else null

            override suspend fun saveHost(host: Host) {}

            override suspend fun deleteHost(uuid: String) {}

            override fun notifyHostUpdated(host: Host) {}
        }

    private val fakeAppRepo =
        object : AppRepository {
            override fun getApps(
                hostUuid: String,
                showHidden: Boolean,
            ): Flow<List<HostApp>> =
                flowOf(listOf(testApp))

            override suspend fun refreshApps(host: Host): Result<List<HostApp>> = Result.success(listOf(testApp))

            override suspend fun setAppHidden(
                hostUuid: String,
                appId: Int,
                hidden: Boolean,
            ) {}

            override suspend fun quitCurrentApp(host: Host): Result<Unit> = Result.success(Unit)

            override fun getBoxArtFile(
                hostUuid: String,
                appId: Int,
            ): File? = null
        }

    private val fakePrefsRepo =
        object : PreferencesRepository {
            private var current =
                StreamSettings(
                    width = 2560,
                    height = 1440,
                    fps = 120,
                    bitrateKbps = 50_000,
                    audioConfig = AudioConfiguration.SURROUND_51,
                    videoFormat = VideoFormatOption.FORCE_AV1,
                    enableHdr = true,
                )
            override val settingsFlow: Flow<StreamSettings> = flowOf(current)

            override suspend fun getSettings(): StreamSettings = current

            override suspend fun updateSettings(transform: (StreamSettings) -> StreamSettings): StreamSettings {
                current = transform(current)
                return current
            }

            override suspend fun setSettings(settings: StreamSettings) {
                current = settings
            }
        }

    private val activeProfileFlow = MutableStateFlow<Profile?>(null)
    private val fakeProfileRepo =
        object : ProfileRepository {
            override val profilesFlow: Flow<List<Profile>> = flowOf(emptyList())
            override val activeProfileFlow: Flow<Profile?> = this@StreamSessionLauncherTest.activeProfileFlow

            override suspend fun getAllProfiles(): List<Profile> = emptyList()

            override suspend fun getProfile(uuid: UUID): Profile? = null

            override suspend fun saveProfile(profile: Profile) {}

            override suspend fun deleteProfile(uuid: UUID) {}

            override suspend fun setActiveProfile(uuid: UUID?) {}
        }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testResolveStreamConfigFromPreferences() = runTest {
        val launcher = StreamSessionLauncher(fakeHostRepo, fakeAppRepo, fakePrefsRepo, fakeProfileRepo)
        val result = launcher.resolveStreamConfig(testHost.uuid, testApp.id)

        assertTrue(result.isSuccess)
        val config = result.getOrThrow()

        assertEquals("192.168.1.50", config.hostAddress)
        assertEquals(47984, config.httpsPort)
        assertEquals(2560, config.width)
        assertEquals(1440, config.height)
        assertEquals(120, config.fps)
        assertEquals(50_000, config.bitrateKbps)
        assertEquals(AudioChannelConfiguration.SURROUND_51, config.audioConfiguration)
        assertEquals(VideoFormat.AV1, config.videoFormat)
        assertTrue(config.enableHdr)
    }

    @Test
    fun testResolveStreamConfigProfileOverrides() = runTest {
        val overrideProfile =
            Profile(
                name = "Handheld Low Battery",
                options = mapOf(
                    "width" to 1280,
                    "height" to 720,
                    "fps" to 60,
                    "bitrate_kbps" to 10_000,
                ),
            )
        activeProfileFlow.value = overrideProfile

        val launcher = StreamSessionLauncher(fakeHostRepo, fakeAppRepo, fakePrefsRepo, fakeProfileRepo)
        val result = launcher.resolveStreamConfig(testHost.uuid, testApp.id)

        assertTrue(result.isSuccess)
        val config = result.getOrThrow()

        assertEquals(1280, config.width)
        assertEquals(720, config.height)
        assertEquals(60, config.fps)
        assertEquals(10_000, config.bitrateKbps)
    }

    @Test
    fun testResolveStreamConfigMissingHostFails() = runTest {
        val launcher = StreamSessionLauncher(fakeHostRepo, fakeAppRepo, fakePrefsRepo, fakeProfileRepo)
        val result = launcher.resolveStreamConfig("non-existent-uuid", testApp.id)
        assertTrue(result.isFailure)
    }

    @Test
    fun testCreateGameActivityIntent() {
        val launcher = StreamSessionLauncher(fakeHostRepo, fakeAppRepo, fakePrefsRepo, fakeProfileRepo)
        val intent = launcher.createGameActivityIntent(context, testHost, testApp)

        assertEquals("192.168.1.50", intent.getStringExtra(Game.EXTRA_HOST))
        assertEquals(47989, intent.getIntExtra(Game.EXTRA_PORT, 0))
        assertEquals(47984, intent.getIntExtra(Game.EXTRA_HTTPS_PORT, 0))
        assertEquals("Steam Big Picture", intent.getStringExtra(Game.EXTRA_APP_NAME))
        assertEquals(101, intent.getIntExtra(Game.EXTRA_APP_ID, 0))
        assertTrue(intent.getBooleanExtra(Game.EXTRA_APP_HDR, false))
        assertEquals("test-host-uuid", intent.getStringExtra(Game.EXTRA_PC_UUID))
        assertEquals("Gaming Rig", intent.getStringExtra(Game.EXTRA_PC_NAME))
    }
}
