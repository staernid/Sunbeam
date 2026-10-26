package io.github.staernid.sunbeam.nvstream.http

import io.github.staernid.sunbeam.TestLogSuppressor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParserException
import java.security.PrivateKey
import java.security.cert.X509Certificate

@Config(sdk = [33], shadows = [io.github.staernid.sunbeam.shadows.ShadowMoonBridge::class])
@RunWith(RobolectricTestRunner::class)
class ServerInfoParserTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun suppressLogs() {
            TestLogSuppressor.install()
        }

        private fun createNvHttp(): NvHTTP {
            val address = ComputerDetails.AddressTuple("127.0.0.1", 47989)
            val dummyCrypto =
                object : LimelightCryptoProvider {
                    override fun getClientCertificate(): X509Certificate? = null

                    override fun getClientPrivateKey(): PrivateKey? = null

                    override fun getPemEncodedClientCertificate(): ByteArray? = null

                    override fun encodeBase64String(data: ByteArray?): String? = null
                }
            return NvHTTP(address, 47984, "test-client-id", null, dummyCrypto)
        }
    }

    private val sunshineServerInfoXml =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <root status_code="200">
            <hostname>SUNSHINE-RIG</hostname>
            <appversion>7.1.431.0</appversion>
            <GfeVersion>3.23.0.74</GfeVersion>
            <uniqueid>0123456789ABCDEF</uniqueid>
            <HttpsPort>47984</HttpsPort>
            <ExternalPort>47989</ExternalPort>
            <mac>aa:bb:cc:dd:ee:ff</mac>
            <LocalIP>192.168.1.150</LocalIP>
            <ExternalIP>203.0.113.42</ExternalIP>
            <PairStatus>1</PairStatus>
            <currentgame>0</currentgame>
            <currentgameuuid></currentgameuuid>
            <state>SUNSHINE_SERVER_FREE</state>
            <MaxLumaPixelsH264>0</MaxLumaPixelsH264>
            <MaxLumaPixelsHEVC>0</MaxLumaPixelsHEVC>
            <ServerCodecModeSupport>3</ServerCodecModeSupport>
            <gputype>NVIDIA GeForce RTX 4090</gputype>
        </root>
        """.trimIndent()

    private val busySunshineServerInfoXml =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <root status_code="200">
            <hostname>SUNSHINE-RIG</hostname>
            <appversion>7.1.431.0</appversion>
            <uniqueid>0123456789ABCDEF</uniqueid>
            <HttpsPort>47984</HttpsPort>
            <ExternalPort>47989</ExternalPort>
            <mac>aa:bb:cc:dd:ee:ff</mac>
            <LocalIP>192.168.1.150</LocalIP>
            <ExternalIP>203.0.113.42</ExternalIP>
            <PairStatus>1</PairStatus>
            <currentgame>42</currentgame>
            <currentgameuuid>game-guid-4242</currentgameuuid>
            <state>SUNSHINE_SERVER_BUSY</state>
        </root>
        """.trimIndent()

    @Test
    fun parseSunshineServerInfo_populatesDetailsCorrectly() {
        val http = createNvHttp()
        val details = http.getComputerDetails(sunshineServerInfoXml)

        assertNotNull(details)
        assertEquals("SUNSHINE-RIG", details.name)
        assertEquals("0123456789ABCDEF", details.uuid)
        assertEquals(47984, details.httpsPort)
        assertEquals(47989, details.externalPort)
        assertEquals("aa:bb:cc:dd:ee:ff", details.macAddress)
        assertEquals("192.168.1.150", details.localAddress.address)
        assertEquals(47989, details.localAddress.port)
        assertEquals("203.0.113.42", details.remoteAddress.address)
        assertEquals(47989, details.remoteAddress.port)
        assertEquals(PairingManager.PairState.PAIRED, details.pairState)
        assertEquals(0, details.runningGameId)
        assertFalse("Official Sunshine server should not be flagged as legacy NVIDIA GFE", details.nvidiaServer)
        assertEquals(ComputerDetails.State.ONLINE, details.state)
    }

    @Test
    fun parseBusySunshineServerInfo_detectsRunningGame() {
        val http = createNvHttp()
        val details = http.getComputerDetails(busySunshineServerInfoXml)

        assertEquals(42, details.runningGameId)
        assertEquals("game-guid-4242", details.runningGameUUID)
    }

    @Test
    fun parseCodecCapabilities_extractsCodecModeAndLuma() {
        val http = createNvHttp()
        assertEquals(3L, http.getServerCodecModeSupport(sunshineServerInfoXml))
        assertEquals(0L, http.getMaxLumaPixelsH264(sunshineServerInfoXml))
        assertEquals(0L, http.getMaxLumaPixelsHEVC(sunshineServerInfoXml))
        assertEquals("NVIDIA GeForce RTX 4090", http.getGpuType(sunshineServerInfoXml))
    }

    @Test
    fun parseUnpairedStatus_reportsNotPaired() {
        val unpairedXml =
            """
            <root status_code="200">
                <uniqueid>ABCD</uniqueid>
                <PairStatus>0</PairStatus>
            </root>
            """.trimIndent()

        val http = createNvHttp()
        assertEquals(PairingManager.PairState.NOT_PAIRED, http.getPairState(unpairedXml))
    }

    @Test(expected = XmlPullParserException::class)
    fun parseMissingUniqueId_throwsException() {
        val invalidXml =
            """
            <root status_code="200">
                <hostname>HOST-WITHOUT-UUID</hostname>
            </root>
            """.trimIndent()

        val http = createNvHttp()
        http.getComputerDetails(invalidXml)
    }

    @Test(expected = HostHttpResponseException::class)
    fun parseErrorStatusCode_throwsHostHttpResponseException() {
        val errorXml =
            """
            <root status_code="401" status_message="Unauthorized">
            </root>
            """.trimIndent()

        val http = createNvHttp()
        http.getComputerDetails(errorXml)
    }
}
