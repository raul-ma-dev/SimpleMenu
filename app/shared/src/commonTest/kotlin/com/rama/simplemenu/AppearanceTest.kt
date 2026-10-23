package com.rama.simplemenu

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.*

class AppearanceTest {
    @Test fun independentCardTextAndNavigationColorsSurviveStorage() {
        val appearance = AppAppearance(
            businessName = "Mi negocio",
            cardBackground = 0xFFF5EA,
            textColor = 0x1B365D,
            navigationBackground = 0x18211E,
        )
        val decoded = decodeShop(ShopData(appearance = appearance).encode()).appearance
        assertEquals(appearance, decoded)
        assertEquals(appearanceColor(0xFFF5EA), cardBackgroundColor(decoded))
        assertEquals(appearanceColor(0x18211E), navigationBackgroundColor(decoded))
        assertEquals(appearanceColor(0x1B365D), appearanceColorScheme(decoded).onBackground)
    }

    @Test fun previousAppearanceRecordsRemainCompatible() {
        val legacy = "C|005400690065006e00640061||#1565C0|#6A1B9A|#18211E\nP|1|00500061006e|1500"
        val decoded = decodeShop(legacy)
        assertEquals("Tienda", decoded.appearance.businessName)
        assertNull(decoded.appearance.cardBackground)
        assertNull(decoded.appearance.textColor)
        assertNull(decoded.appearance.navigationBackground)
        assertEquals(0x1565C0, decoded.appearance.primary)
        assertEquals("Pan", decoded.products.single().name)
        assertEquals(decoded, decodeShop(decoded.encode()))
    }

    @Test fun textStaysReadableAcrossIndependentlyChosenSurfaces() {
        val appearance = AppAppearance(background = 0xFFFFFF, cardBackground = 0x000000, textColor = 0xFFFFFF, navigationBackground = 0x1565C0)
        val preferred = preferredTextColor(appearance)
        listOf(appearanceColor(appearance.background), cardBackgroundColor(appearance), navigationBackgroundColor(appearance)).forEach { background ->
            val text = legibleAccent(preferred, background)
            val ratio = (max(text.luminance(), background.luminance()) + .05f) / (min(text.luminance(), background.luminance()) + .05f)
            assertTrue(ratio >= 4.5f)
        }
    }

    @Test fun appearanceSurvivesStorageWithoutChangingProductsOrSales() {
        val appearance = AppAppearance("Café | María ☕", "logo-1234.png", 0x1565C0, 0x6A1B9A, 0x18211E)
        val shop = ShopData(listOf(Product(1, "Pan", 1500)), listOf(Sale(1, "2026-10-21", listOf(SaleLine("Pan", 1200, 2)))), appearance)
        assertEquals(shop, decodeShop(shop.encode()))
        assertEquals(appearance, decodeShop(shop.deleteSale(1).encode()).appearance)
        assertEquals(appearance, decodeShop(shop.copy(products = emptyList()).encode()).appearance)
    }

    @Test fun oldDataUsesDefaultAppearance() {
        val legacy = decodeShop("P|1|00500061006e|1500")
        assertEquals(AppAppearance(), legacy.appearance)
        assertEquals("Pan", legacy.products.single().name)
        assertEquals(legacy, decodeShop(legacy.encode()))
    }

    @Test fun hexColorsMustContainSixDigits() {
        assertEquals(0xABCDEF, parseHexColor(" #abcdef "))
        assertEquals(0x146C59, parseHexColor("146C59"))
        assertEquals("#00000A", hexColor(10))
        assertNull(parseHexColor("#FFF"))
        assertNull(parseHexColor("#GGGGGG"))
        assertNull(parseHexColor("#FF000000"))
    }

    @Test fun matchingAccentAndBackgroundStillProduceReadableLabels() {
        listOf(Color.White, Color.Black, appearanceColor(0x1565C0), appearanceColor(0xF7F9F6)).forEach { background ->
            val accent = legibleAccent(background, background)
            val ratio = (max(accent.luminance(), background.luminance()) + .05f) / (min(accent.luminance(), background.luminance()) + .05f)
            assertTrue(ratio >= 4.5f)
            val scheme = appearanceColorScheme(AppAppearance(background = if (background == Color.Black) 0 else 0xFFFFFF))
            assertTrue(scheme.onBackground != scheme.background)
        }
    }
}
