package com.ramasimple

import kotlin.test.*

class DailySalesPdfTest {
    private val measure: (String, Float, Boolean) -> Float = { text, size, _ -> text.length * size * .6f }

    @Test fun pdfContainsOnlyTheSelectedDayAndCorrectTotals() {
        val pages = dailyPdfPages("2026-10-21", listOf(
            Sale(1, "2026-10-21", listOf(SaleLine("Café", 1250, 3))),
            Sale(2, "2026-10-22", listOf(SaleLine("No incluir", 500, 1))),
        ), measure)
        val texts = pages.flatten().filterIsInstance<ReportText>().map { it.value }
        assertEquals(1, pages.size)
        assertTrue("Café" in texts)
        assertTrue("$37.50" in texts)
        assertFalse(texts.any { "No incluir" in it || "2026-10-22" in it })
    }

    @Test fun longOrdersPaginateAndKeepTextInsideThePage() {
        val sale = Sale(1, "2026-10-21", List(120) { SaleLine("Producto $it con un nombre que requiere más de un renglón", 1234, 3) })
        val pages = dailyPdfPages(sale.day, listOf(sale), measure)
        assertTrue(pages.size > 1)
        pages.forEachIndexed { index, page ->
            val texts = page.filterIsInstance<ReportText>()
            assertTrue(texts.any { it.value == "Página ${index + 1} de ${pages.size}" })
            assertTrue(texts.any { it.value.startsWith("Pedido #1") })
            texts.forEach { assertTrue(it.y >= 0f && it.y + it.size * 1.3f < REPORT_HEIGHT) }
            page.filterIsInstance<ReportBox>().forEach { assertTrue(it.y + it.height <= REPORT_HEIGHT) }
        }
        assertEquals(120, pages.flatten().filterIsInstance<ReportText>().count { it.value.startsWith("Producto ") })
    }

    @Test fun longWordsWrapWithoutLosingCharacters() {
        val name = "Café☕".repeat(80)
        val wrapped = wrapReportText(name, 100f) { it.length * 6f }
        assertEquals(name, wrapped.joinToString(""))
        assertTrue(wrapped.all { it.length * 6 <= 100 })
    }
}
