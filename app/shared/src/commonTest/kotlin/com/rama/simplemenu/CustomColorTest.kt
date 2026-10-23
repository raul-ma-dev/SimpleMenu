package com.rama.simplemenu

import androidx.compose.ui.graphics.Color
import kotlin.test.*

class CustomColorTest {
    @Test fun selectorStartsAtTheExistingColor() {
        listOf(0x000000L, 0xFFFFFFL, 0x808080L, 0xFF0000L, 0x00FF00L, 0x0000FFL, 0x146C59L, 0xABCDEFL).forEach { rgb ->
            val hsv = colorToHsv(appearanceColor(rgb))
            assertEquals(rgb, Color.hsv(hsv.hue, hsv.saturation, hsv.brightness).rgbValue())
        }
    }

    @Test fun blackAndGrayDoNotProduceInvalidHueOrSaturation() {
        val black = colorToHsv(Color.Black)
        assertEquals(0f, black.saturation)
        assertEquals(0f, black.brightness)
        val gray = colorToHsv(appearanceColor(0x808080))
        assertEquals(0f, gray.saturation)
        assertTrue(gray.hue in 0f..360f)
        assertTrue(gray.brightness in 0f..1f)
    }
}
