@file:Suppress(
    "TooGenericExceptionCaught",
    "SwallowedException",
    "MagicNumber",
    "NestedBlockDepth",
    "LongMethod",
    "CyclomaticComplexMethod",
)

package io.github.staernid.sunbeam.core.data.parser

import io.github.staernid.sunbeam.core.data.model.SunshineServerCapabilities
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.ArrayDeque

/**
 * Parser for Sunshine /serverinfo XML responses into typed [SunshineServerCapabilities].
 */
object SunshineServerInfoParser {
    fun parse(xmlContent: String?): SunshineServerCapabilities {
        if (xmlContent.isNullOrBlank()) {
            return SunshineServerCapabilities.DEFAULT
        }

        var codecMode = 0
        var appVersion: String? = null
        var gfeVersion: String? = null
        var gpuType: String? = null
        var maxLuma = 0L
        var isSunshine = true

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            val currentTag = ArrayDeque<String>()
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag.push(parser.name)
                    }
                    XmlPullParser.END_TAG -> {
                        if (currentTag.isNotEmpty()) {
                            currentTag.pop()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty() && currentTag.isNotEmpty()) {
                            when (currentTag.peek()) {
                                "ServerCodecModeSupport" -> {
                                    codecMode = text.toIntOrNull() ?: codecMode
                                }
                                "appversion" -> {
                                    appVersion = text
                                }
                                "GfeVersion" -> {
                                    gfeVersion = text
                                }
                                "gputype" -> {
                                    gpuType = text
                                }
                                "MaxLumaPixelsHEVC" -> {
                                    maxLuma = text.toLongOrNull() ?: maxLuma
                                }
                                "state" -> {
                                    if (text.contains("MJOLNIR", ignoreCase = true)) {
                                        isSunshine = false
                                    }
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Return parsed so far or default
        }

        return SunshineServerCapabilities(
            serverCodecModeSupport = codecMode,
            appVersion = appVersion,
            gfeVersion = gfeVersion,
            gpuType = gpuType,
            maxLumaPixelsHevc = maxLuma,
            isSunshine = isSunshine,
        )
    }
}
