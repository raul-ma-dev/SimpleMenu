package com.rama.simplemenu

import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.io.File
import java.util.Base64
import kotlin.test.*

class DailyPdfLayoutPreviewTest {
    @Test fun writesPaginatedLayoutForVisualReview() {
        val day = "2026-10-21"
        val company = "Café y Panadería La Esquina de Doña María"
        val sales = (1L..2L).map { id -> Sale(id, day, List(8) { index ->
            SaleLine("Artículo ${index + 1}: café de olla con pan dulce especial", 2550, index % 3 + 1, "Bebidas y alimentos preparados", "Desayunos especiales de la casa")
        }) }
        val context = FontRenderContext(AffineTransform(), true, true)
        val pages = dailyPdfPages(day, sales, measure = { text, size, bold ->
            Font("Arial", if (bold) Font.BOLD else Font.PLAIN, 1).deriveFont(size).getStringBounds(text, context).width.toFloat()
        }, businessName = company)
        assertTrue(pages.size > 1)
        val output = File("build/reports/pdf/layout-preview.tsv")
        output.parentFile?.mkdirs()
        output.bufferedWriter().use { writer ->
            pages.forEach { commands ->
                writer.appendLine("P")
                commands.forEach { command ->
                    val row = when (command) {
                        is ReportText -> listOf("T", Base64.getEncoder().encodeToString(command.value.toByteArray(Charsets.UTF_8)), command.x, command.y, command.size, command.bold, command.color.red, command.color.green, command.color.blue)
                        is ReportBox -> listOf("R", command.x, command.y, command.width, command.height, command.color.red, command.color.green, command.color.blue)
                    }
                    writer.appendLine(row.joinToString("\t"))
                }
            }
        }
    }
}
