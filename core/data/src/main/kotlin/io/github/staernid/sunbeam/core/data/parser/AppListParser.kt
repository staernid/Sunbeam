@file:Suppress(
    "TooGenericExceptionCaught",
    "SwallowedException",
    "NestedBlockDepth",
    "CyclomaticComplexMethod",
    "LongMethod",
)

package io.github.staernid.sunbeam.core.data.parser

import io.github.staernid.sunbeam.core.data.model.HostApp
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.io.StringReader
import java.util.ArrayDeque

/**
 * Parser for Sunshine and GameStream `/applist` XML responses into typed [HostApp] models.
 */
object AppListParser {
    /**
     * Parses the given XML string into a list of [HostApp]s.
     * Returns an empty list if [xmlContent] is null or blank.
     * Throws [XmlPullParserException] if the XML root tag is unclosed or malformed.
     */
    fun parse(xmlContent: String?): List<HostApp> {
        if (xmlContent.isNullOrBlank()) {
            return emptyList()
        }
        return parse(StringReader(xmlContent))
    }

    /**
     * Parses from a [Reader] into a list of [HostApp]s.
     */
    fun parse(reader: Reader): List<HostApp> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(reader)

        val apps = mutableListOf<HostApp>()
        val currentTag = ArrayDeque<String>()
        var rootTerminated = false

        var currentTitle: String? = null
        var currentId: Int? = null
        var currentUuid: String? = null
        var currentIndex: Int? = null
        var currentIsHdr = false

        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    currentTag.push(name)
                    if (name.equals("App", ignoreCase = true)) {
                        currentTitle = null
                        currentId = null
                        currentUuid = null
                        currentIndex = null
                        currentIsHdr = false
                    }
                }

                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (currentTag.isNotEmpty()) {
                        currentTag.pop()
                    }
                    if (name.equals("root", ignoreCase = true)) {
                        rootTerminated = true
                    } else if (name.equals("App", ignoreCase = true)) {
                        val id = currentId
                        val title = currentTitle
                        if (id != null && !title.isNullOrBlank()) {
                            apps.add(
                                HostApp(
                                    id = id,
                                    title = title,
                                    uuid = currentUuid ?: "",
                                    index = currentIndex ?: 0,
                                    isHdrSupported = currentIsHdr,
                                ),
                            )
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim()
                    if (!text.isNullOrEmpty() && currentTag.isNotEmpty()) {
                        when (currentTag.peek()) {
                            "AppTitle" -> currentTitle = text
                            "ID" -> currentId = text.toIntOrNull()
                            "UUID" -> currentUuid = text
                            "IDX" -> currentIndex = text.toIntOrNull()
                            "IsHdrSupported" -> currentIsHdr = text == "1"
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (!rootTerminated) {
            throw XmlPullParserException("Malformed XML: Root tag was not terminated")
        }

        return apps
    }
}
