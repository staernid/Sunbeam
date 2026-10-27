package io.github.staernid.sunbeam.core.data

import io.github.staernid.sunbeam.core.data.parser.AppListParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParserException

@RunWith(RobolectricTestRunner::class)
class AppListParserTest {
    @Test
    fun `parse valid xml returns list of apps`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <root status_code="200">
                <App>
                    <AppTitle>Steam Big Picture</AppTitle>
                    <ID>1001</ID>
                    <UUID>steam-guid-1234</UUID>
                    <IDX>0</IDX>
                    <IsHdrSupported>1</IsHdrSupported>
                </App>
                <App>
                    <AppTitle>Desktop</AppTitle>
                    <ID>1002</ID>
                    <UUID>desktop-guid-5678</UUID>
                    <IDX>1</IDX>
                    <IsHdrSupported>0</IsHdrSupported>
                </App>
            </root>
        """.trimIndent()

        val apps = AppListParser.parse(xml)

        assertEquals(2, apps.size)

        val steam = apps[0]
        assertEquals(1001, steam.id)
        assertEquals("Steam Big Picture", steam.title)
        assertEquals("steam-guid-1234", steam.uuid)
        assertEquals(0, steam.index)
        assertTrue(steam.isHdrSupported)

        val desktop = apps[1]
        assertEquals(1002, desktop.id)
        assertEquals("Desktop", desktop.title)
        assertEquals("desktop-guid-5678", desktop.uuid)
        assertEquals(1, desktop.index)
        assertFalse(desktop.isHdrSupported)
    }

    @Test
    fun `parse empty or null xml returns empty list`() {
        assertTrue(AppListParser.parse(null).isEmpty())
        assertTrue(AppListParser.parse("").isEmpty())
        assertTrue(AppListParser.parse("   ").isEmpty())
    }

    @Test
    fun `parse skips apps missing id or title`() {
        val xml = """
            <root status_code="200">
                <App>
                    <AppTitle>Valid App</AppTitle>
                    <ID>50</ID>
                </App>
                <App>
                    <ID>51</ID>
                </App>
                <App>
                    <AppTitle>Missing ID</AppTitle>
                </App>
            </root>
        """.trimIndent()

        val apps = AppListParser.parse(xml)
        assertEquals(1, apps.size)
        assertEquals(50, apps[0].id)
        assertEquals("Valid App", apps[0].title)
    }

    @Test
    fun `parse throws XmlPullParserException on unclosed root tag`() {
        val invalidXml = """
            <root status_code="200">
                <App>
                    <AppTitle>Broken</AppTitle>
                    <ID>1</ID>
                </App>
        """.trimIndent()

        try {
            AppListParser.parse(invalidXml)
            fail("Expected XmlPullParserException")
        } catch (_: XmlPullParserException) {
            // Success
        }
    }
}
