package io.github.staernid.sunbeam.core.data

import io.github.staernid.sunbeam.core.data.model.Profile
import io.github.staernid.sunbeam.core.data.repository.JsonProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class JsonProfileRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var profilesFile: File
    private lateinit var repository: JsonProfileRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        profilesFile = File(tempFolder.root, "profiles.json")
        repository =
            JsonProfileRepository(
                profilesFile = profilesFile,
                ioDispatcher = testDispatcher,
            )
    }

    @Test
    fun saveAndGetProfile_updatesFlows() =
        runTest(testDispatcher) {
            val id = UUID.randomUUID()
            val profile =
                Profile(
                    uuid = id,
                    name = "Low Latency 120FPS",
                    options = mapOf("fps" to 120, "enableHdr" to true),
                )

            repository.saveProfile(profile)

            val retrieved = repository.getProfile(id)
            assertNotNull(retrieved)
            assertEquals("Low Latency 120FPS", retrieved?.name)
            assertEquals(120.0, (retrieved?.options?.get("fps") as? Number)?.toDouble())

            val allProfiles = repository.getAllProfiles()
            assertEquals(1, allProfiles.size)

            val flowProfiles = repository.profilesFlow.first()
            assertEquals(1, flowProfiles.size)
        }

    @Test
    fun setActiveProfile_emitsActiveProfile() =
        runTest(testDispatcher) {
            val id = UUID.randomUUID()
            val profile =
                Profile(
                    uuid = id,
                    name = "Cinema 4K",
                )

            repository.saveProfile(profile)
            assertNull(repository.activeProfileFlow.first())

            repository.setActiveProfile(id)
            val active = repository.activeProfileFlow.first()
            assertNotNull(active)
            assertEquals(id, active?.uuid)
            assertTrue(active?.isActive == true)

            repository.deleteProfile(id)
            assertNull(repository.activeProfileFlow.first())
            assertEquals(0, repository.getAllProfiles().size)
        }
}
