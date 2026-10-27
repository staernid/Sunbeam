package io.github.staernid.sunbeam.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.nvstream.http.ComputerDetails
import io.github.staernid.sunbeam.nvstream.http.NvHTTP
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [io.github.staernid.sunbeam.shadows.ShadowMoonBridge::class])
class NvAppNetworkDataSourceTest {
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun fetchAppListXml_usesHostHttpsPortInsteadOfAddressPort() = runTest(testDispatcher) {
        var capturedAddress: ComputerDetails.AddressTuple? = null
        var capturedHttpsPort: Int? = null

        val mockHttp = mock(NvHTTP::class.java)
        `when`(mockHttp.appListRaw).thenReturn("<root></root>")

        val dataSource = NvAppNetworkDataSource(
            context = context,
            ioDispatcher = testDispatcher,
            httpFactory = { address, httpsPort, _, _, _ ->
                capturedAddress = address
                capturedHttpsPort = httpsPort
                mockHttp
            },
        )

        val host = Host(
            uuid = "test-uuid",
            name = "Test Host",
            localAddress = HostAddress("192.168.178.29", 47989),
            httpsPort = 47984,
        )

        val result = dataSource.fetchAppListXml(host)
        assertEquals(true, result.isSuccess)
        assertEquals(47989, capturedAddress?.port)
        // Must use httpsPort 47984, NOT address.port (47989)
        assertEquals(47984, capturedHttpsPort)
    }

    @Test
    fun fetchAppListXml_preservesZeroHttpsPortWhenUnknown() = runTest(testDispatcher) {
        var capturedHttpsPort: Int? = null

        val mockHttp = mock(NvHTTP::class.java)
        `when`(mockHttp.appListRaw).thenReturn("<root></root>")

        val dataSource = NvAppNetworkDataSource(
            context = context,
            ioDispatcher = testDispatcher,
            httpFactory = { _, httpsPort, _, _, _ ->
                capturedHttpsPort = httpsPort
                mockHttp
            },
        )

        val host = Host(
            uuid = "test-uuid",
            name = "Test Host",
            localAddress = HostAddress("192.168.178.29", 47989),
            httpsPort = 0,
        )

        val result = dataSource.fetchAppListXml(host)
        assertEquals(true, result.isSuccess)
        // When host httpsPort is 0, must pass 0 to let NvHTTP dynamically discover it via serverinfo
        assertEquals(0, capturedHttpsPort)
    }
}
