package com.rama.simplemenu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopTest {
    @Test fun readsProductsSavedBeforeCategoriesWereAdded() {
        // Original format: name is UTF-16 hex, followed by the price in cents.
        val legacy = "P|1|00500061006e|1050\nS|1|2026-10-21\nL|00500061006e|1050|2"
        val shop = decodeShop(legacy)
        assertEquals(Product(1, "Pan", 1050), shop.products.single())
        assertEquals(2100L, shop.sales.single().total)
        assertEquals(shop, decodeShop(shop.encode()))
    }

    @Test fun categoriesAndPhotoSurviveStorage() {
        val product = Product(1, "Café", 1500, "Bebidas | café", "Frías\n☕", "product-1234.jpg")
        val shop = ShopData(listOf(product, Product(2, "Pan", 1000, "Comida")))
        assertEquals(shop, decodeShop(shop.encode()))
    }

    @Test fun subcategoryFilterStaysWithinItsParentCategory() {
        val coffee = Product(1, "Café", 1500, "Bebidas", "Especiales")
        val bread = Product(2, "Pan", 1000, "Comida", "Especiales")
        val water = Product(3, "Agua", 800, "Bebidas", "")
        val legacy = Product(4, "Otro", 500)
        val products = listOf(coffee, bread, water, legacy)
        assertEquals(listOf(coffee), products.inCategory("Bebidas", "Especiales"))
        assertEquals(listOf(coffee, water), products.inCategory("Bebidas", null))
        assertEquals(listOf(water), products.inCategory("Bebidas", ""))
        assertEquals(listOf(legacy), products.inCategory("", null))
        assertEquals(products, products.inCategory(null, null))
    }

    @Test fun pricesUseExactCents() {
        assertEquals(1234L, parsePrice("12,34"))
        assertEquals(10L, parsePrice("0.1"))
        assertNull(parsePrice("-1"))
        assertNull(parsePrice("1.234"))
        assertNull(parsePrice("0"))
    }
    @Test fun storagePreservesNamesAndHistoricalPrices() {
        val sale = Sale(1, "2026-10-21", listOf(SaleLine("Café | especial\n☕", 1250, 3)))
        val shop = ShopData(listOf(Product(1, "Café", 1500)), listOf(sale))
        assertEquals(shop, decodeShop(shop.encode()))
        assertEquals(3750L, decodeShop(shop.encode()).sales.single().total)
    }
    @Test fun dailyTotalsKeepDatesSeparate() {
        val sales = listOf(Sale(1, "2026-10-21", listOf(SaleLine("Pan", 1050, 2))), Sale(2, "2026-10-22", listOf(SaleLine("Pan", 1050, 1))))
        assertEquals(2100L, sales.filter { it.day == "2026-10-21" }.sumOf { it.total })
    }
}
