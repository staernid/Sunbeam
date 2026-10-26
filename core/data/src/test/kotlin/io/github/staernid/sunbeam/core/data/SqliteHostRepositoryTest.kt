package io.github.staernid.sunbeam.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.repository.SqliteHostRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SqliteHostRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: SqliteHostRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("test_computers.db")
        repository =
            SqliteHostRepository(
                context = context,
                dbName = "test_computers.db",
                ioDispatcher = testDispatcher,
            )
    }

    @Test
    fun saveAndRetrieveHost_persistsAttributes() =
        runTest(testDispatcher) {
            val host =
                Host(
                    uuid = "test-uuid-1",
                    name = "Gaming Rig",
                    localAddress = HostAddress("192.168.1.50", 47989),
                    remoteAddress = HostAddress("myhost.duckdns.org", 47984),
                    macAddress = "00:11:22:33:44:55",
                    serverCertDer = byteArrayOf(1, 2, 3, 4, 5),
                )

            repository.saveHost(host)

            val retrieved = repository.getHostByUuid("test-uuid-1")
            assertNotNull(retrieved)
            assertEquals("test-uuid-1", retrieved?.uuid)
            assertEquals("Gaming Rig", retrieved?.name)
            assertEquals("192.168.1.50", retrieved?.localAddress?.address)
            assertEquals(47989, retrieved?.localAddress?.port)
            assertEquals("myhost.duckdns.org", retrieved?.remoteAddress?.address)
            assertEquals(47984, retrieved?.remoteAddress?.port)
            assertEquals("00:11:22:33:44:55", retrieved?.macAddress)

            val allHosts = repository.getAllHosts()
            assertEquals(1, allHosts.size)

            val flowHosts = repository.hostsFlow.first()
            assertEquals(1, flowHosts.size)
            assertEquals("Gaming Rig", flowHosts[0].name)
        }

    @Test
    fun deleteHost_removesFromDatabaseAndEmitsUpdate() =
        runTest(testDispatcher) {
            val host =
                Host(
                    uuid = "test-uuid-2",
                    name = "Media PC",
                )

            repository.saveHost(host)
            assertEquals(1, repository.getAllHosts().size)

            repository.deleteHost("test-uuid-2")
            assertNull(repository.getHostByUuid("test-uuid-2"))
            assertEquals(0, repository.getAllHosts().size)
            assertEquals(0, repository.hostsFlow.first().size)
        }
}
