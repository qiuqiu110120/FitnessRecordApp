package com.example.fitnessrecord.ui.theme

import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorTest {
    @Test
    fun rgbInputNormalizesWithoutColorRoundTrip() {
        assertEquals("#1565C0", normalizeThemeColorInput("rgb(21, 101, 192)"))
        assertEquals("#1565C0", normalizeThemeColorInput("RGB( 21 , 101 , 192 )"))
    }

    @Test
    fun hexInputNormalizesToCanonicalForm() {
        assertEquals("#1565C0", normalizeThemeColorInput("1565c0"))
        assertEquals("#1565C0", normalizeThemeColorInput(" #1565c0 "))
    }

    @Test
    fun persistedDecoderOnlyAcceptsCanonicalHex() {
        assertEquals("#1565C0", decodePersistedThemeColor("#1565C0"))
        assertNull(decodePersistedThemeColor("1565C0"))
        assertNull(decodePersistedThemeColor("#1565c0"))
        assertNull(decodePersistedThemeColor("rgb(21,101,192)"))
        assertNull(decodePersistedThemeColor("#FF1565C0"))
    }

    @Test
    fun standardHexCreatesExpectedSrgbChannels() {
        val color = themeSeedColor("#1565C0")
        assertEquals(21, (color.red * 255).roundToInt())
        assertEquals(101, (color.green * 255).roundToInt())
        assertEquals(192, (color.blue * 255).roundToInt())
        assertEquals(255, (color.alpha * 255).roundToInt())
    }

    @Test
    fun invalidRgbValuesAreRejected() {
        assertTrue(parseThemeColorInput("rgb(256,0,0)") is ThemeColorParseResult.ChannelOutOfRange)
        assertTrue(
            parseThemeColorInput("rgb(999999999999999999999,0,0)") is
                ThemeColorParseResult.ChannelOutOfRange
        )
        assertTrue(parseThemeColorInput("rgb(-1,0,0)") is ThemeColorParseResult.UnsupportedFormat)
        assertNull(normalizeThemeColorInput("21,101,192"))
        assertNull(normalizeThemeColorInput("rgb (21,101,192)"))
    }

    @Test
    fun onlyAsciiSpacesAreAccepted() {
        assertEquals("#1565C0", normalizeThemeColorInput(" #1565C0 "))
        assertNull(normalizeThemeColorInput("\t#1565C0\t"))
        assertNull(normalizeThemeColorInput("\n#1565C0\n"))
        assertNull(normalizeThemeColorInput("\u00A0#1565C0\u00A0"))
        assertNull(normalizeThemeColorInput("\u3000#1565C0\u3000"))
        assertNull(normalizeThemeColorInput("\u200B#1565C0\u200B"))
    }

    @Test
    fun excessiveInputIsRejectedBeforeTrimming() {
        assertTrue(parseThemeColorInput(" ".repeat(129)) is ThemeColorParseResult.TooLong)
    }

    @Test
    fun presetsAndCanonicalCustomColorsAreValidSelections() {
        assertEquals("green", normalizeThemeColorSelectionForStorage("green"))
        assertEquals("#1565C0", normalizeThemeColorSelectionForStorage("#1565C0"))
        assertNull(normalizeThemeColorSelectionForStorage("#1565c0"))
        assertFalse(isCustomThemeColor("green"))
        assertTrue(isCustomThemeColor("#1565C0"))
    }
}
