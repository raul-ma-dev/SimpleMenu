package com.rama.simplemenu

import kotlin.test.*

class SalesLogicTest {
    private val day = "2026-10-21"
    private val first = Sale(1, day, listOf(SaleLine("Café", 1250, 2)))
    private val second = Sale(2, day, listOf(SaleLine("Pan", 1000, 1)))
    private val otherDay = Sale(3, "2026-10-22", listOf(SaleLine("Agua", 800, 1)))
    private val shop = ShopData(listOf(Product(1, "Café", 1500)), listOf(first, second, otherDay))

    @Test fun editingChangesOnlyTheSelectedSaleAndDailyTotal() {
        val edited = first.copy(lines = listOf(SaleLine("Café", 1250, 3), SaleLine("Extra", 250, 1)))
        val saved = decodeShop(shop.updateSale(edited).encode())
        assertEquals(5000L, saved.sales.filter { it.day == day }.sumOf { it.total })
        assertEquals(edited, saved.sales.first())
        assertEquals(second, saved.sales[1])
        assertEquals(otherDay, saved.sales[2])
        assertEquals(shop.products, saved.products)
    }

    @Test fun deletingKeepsOtherSalesAndRecalculatesTotal() {
        val saved = decodeShop(shop.deleteSale(first.id).encode())
        assertEquals(listOf(second, otherDay), saved.sales)
        assertEquals(1000L, saved.sales.filter { it.day == day }.sumOf { it.total })
        assertEquals(shop.products, saved.products)
    }

    @Test fun invalidSaleCannotBeSaved() {
        assertFailsWith<IllegalArgumentException> { shop.updateSale(first.copy(lines = emptyList())) }
        assertFailsWith<IllegalArgumentException> { shop.updateSale(first.copy(day = "2026-10-22")) }
        assertNull(SaleLineDraft("Café", "1.234", "1").toSaleLine())
        assertNull(SaleLineDraft("Café", "12.50", "0").toSaleLine())
        assertNull(SaleLineDraft("Café", "12.50", "1000").toSaleLine())
        assertNull(SaleLineDraft("  ", "12.50", "1").toSaleLine())
        assertEquals(first.lines.single(), first.lines.single().toDraft().toSaleLine())
    }

    @Test fun reportUsesTheSelectedDayAndEditedValues() {
        val changed = shop.updateSale(first.copy(lines = listOf(SaleLine("Café", 1250, 3)))).deleteSale(second.id)
        val csv = dailySalesCsv(day, changed.sales)
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("\"Artículo\",\"$day\",\"1\",\"Café\",\"3\",\"12.50\",\"37.50\""))
        assertTrue(csv.contains("\"Total del día\",\"$day\",\"\",\"\",\"3\",\"\",\"37.50\""))
        assertFalse(csv.contains("2026-10-22"))
        assertFalse(csv.contains("Pan"))
    }

    @Test fun reportEscapesNamesAndNeutralizesSpreadsheetFormulas() {
        val sale = Sale(1, day, listOf(SaleLine("Pan, \"especial\"\nchico", 10, 1), SaleLine("=1+1", 20, 1)))
        val csv = dailySalesCsv(day, listOf(sale))
        assertTrue(csv.contains("\"Pan, \"\"especial\"\"\nchico\""))
        assertTrue(csv.contains("\"'=1+1\""))
        assertTrue(csv.contains("\"0.30\""))
    }
}
