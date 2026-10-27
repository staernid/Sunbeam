package io.github.staernid.sunbeam.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.repository.AppNetworkDataSource
import io.github.staernid.sunbeam.core.data.repository.CachedAppRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CachedAppRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var cacheDir: File
    private val testDispatcher = StandardTestDispatcher()

    private val sampleXml = """
        <root status_code="200">
            <App>
                <AppTitle>Cyberpunk 2077</AppTitle>
                <ID>101</ID>
                <UUID>cp-2077</UUID>
                <IsHdrSupported>1</IsHdrSupported>
            </App>
            <App>
                <AppTitle>Hades II</AppTitle>
                <ID>102</ID>
                <UUID>hades-2</UUID>
                <IsHdrSupported>0</IsHdrSupported>
            </App>
        </root>
    """.trimIndent()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        cacheDir = tempFolder.newFolder("cache")
    }

    @Test
    fun `getApps loads from disk cache when file exists`() = runTest(testDispatcher) {
        val hostUuid = "test-host-uuid"
        val appListDir = File(cacheDir, "applist").apply { mkdirs() }
        File(appListDir, hostUuid).writeText(sampleXml)

        val repo = CachedAppRepository(context, cacheDir = cacheDir, ioDispatcher = testDispatcher)
        val apps = repo.getApps(hostUuid).first()

        assertEquals(2, apps.size)
        assertEquals("Cyberpunk 2077", apps[0].title)
        assertEquals(101, apps[0].id)
        assertTrue(apps[0].isHdrSupported)
        assertEquals("Hades II", apps[1].title)
        assertEquals(102, apps[1].id)
    }

    @Test
    fun `getApps filters hidden apps when showHidden is false`() = runTest(testDispatcher) {
        val hostUuid = "host-with-hidden"
        val appListDir = File(cacheDir, "applist").apply { mkdirs() }
        File(appListDir, hostUuid).writeText(sampleXml)

        val repo = CachedAppRepository(context, cacheDir = cacheDir, ioDispatcher = testDispatcher)

        // Hide Hades II (102)
        repo.setAppHidden(hostUuid, 102, hidden = true)

        val visibleApps = repo.getApps(hostUuid, showHidden = false).first()
        assertEquals(1, visibleApps.size)
        assertEquals(101, visibleApps[0].id)

        val allApps = repo.getApps(hostUuid, showHidden = true).first()
        assertEquals(2, allApps.size)
        val hiddenApp = allApps.first { it.id == 102 }
        assertTrue(hiddenApp.isHidden)

        // Unhide Hades II
        repo.setAppHidden(hostUuid, 102, hidden = false)
        val unhiddenApps = repo.getApps(hostUuid, showHidden = false).first()
        assertEquals(2, unhiddenApps.size)
    }

    @Test
    fun `getBoxArtFile returns file path when boxart exists`() = runTest(testDispatcher) {
        val hostUuid = "boxart-host"
        val boxArtDir = File(cacheDir, "boxart/$hostUuid").apply { mkdirs() }
        val boxArtFile = File(boxArtDir, "101.png").apply { writeText("png-bytes") }

        val repo = CachedAppRepository(context, cacheDir = cacheDir, ioDispatcher = testDispatcher)
        val resolved = repo.getBoxArtFile(hostUuid, 101)

        assertNotNull(resolved)
        assertEquals(boxArtFile.absolutePath, resolved?.absolutePath)
        assertNull(repo.getBoxArtFile(hostUuid, 999))
    }

    @Test
    fun `refreshApps writes network result to cache and emits updated list`() = runTest(testDispatcher) {
        val host = Host(uuid = "net-host", name = "Gaming Rig")
        val fakeNetwork = object : AppNetworkDataSource {
            override suspend fun fetchAppListXml(host: Host): Result<String> {
                return Result.success(sampleXml)
            }

            override suspend fun quitApp(host: Host): Result<Unit> {
                return Result.success(Unit)
            }
        }

        val repo = CachedAppRepository(
            context,
            cacheDir = cacheDir,
            networkDataSource = fakeNetwork,
            ioDispatcher = testDispatcher,
        )

        val result = repo.refreshApps(host)
        assertTrue(result.isSuccess)

        val cachedFile = File(cacheDir, "applist/${host.uuid}")
        assertTrue(cachedFile.exists())
        assertEquals(sampleXml, cachedFile.readText())

        val apps = repo.getApps(host.uuid).first()
        assertEquals(2, apps.size)
    }
}
