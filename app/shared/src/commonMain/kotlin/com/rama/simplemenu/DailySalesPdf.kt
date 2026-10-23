package com.rama.simplemenu

internal const val REPORT_WIDTH = 595
internal const val REPORT_HEIGHT = 842
internal data class ReportColor(val red: Float, val green: Float, val blue: Float)
internal val reportInk = ReportColor(.12f, .19f, .17f)
internal val reportGreen = ReportColor(.08f, .42f, .35f)
internal val reportPale = ReportColor(.92f, .96f, .94f)
internal sealed interface ReportDraw
internal data class ReportText(val value: String, val x: Float, val y: Float, val size: Float, val bold: Boolean = false, val color: ReportColor = reportInk) : ReportDraw
internal data class ReportBox(val x: Float, val y: Float, val width: Float, val height: Float, val color: ReportColor) : ReportDraw

expect fun dailySalesPdf(day: String, sales: List<Sale>): ByteArray

internal fun wrapReportText(text: String, width: Float, measure: (String) -> Float): List<String> {
    val result = mutableListOf<String>()
    var line = ""
    text.split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { word ->
        val candidate = if (line.isEmpty()) word else "$line $word"
        if (measure(candidate) <= width) line = candidate
        else {
            if (line.isNotEmpty()) { result.add(line); line = "" }
            // Split a long word without separating UTF-16 surrogate pairs.
            var index = 0
            while (index < word.length) {
                val length = if (word[index].code in 0xD800..0xDBFF && index + 1 < word.length && word[index + 1].code in 0xDC00..0xDFFF) 2 else 1
                val glyph = word.substring(index, index + length)
                if (line.isNotEmpty() && measure(line + glyph) > width) { result.add(line); line = "" }
                line += glyph
                index += length
            }
        }
    }
    if (line.isNotEmpty()) result.add(line)
    return result.ifEmpty { listOf("") }
}

internal fun dailyPdfPages(day: String, sales: List<Sale>, measure: (String, Float, Boolean) -> Float): List<List<ReportDraw>> {
    val daily = sales.filter { it.day == day }.sortedBy { it.id }
    val pages = mutableListOf<MutableList<ReportDraw>>()
    var page = mutableListOf<ReportDraw>()
    var y = 0f
    fun text(value: String, x: Float, top: Float, size: Float = 10f, bold: Boolean = false, color: ReportColor = reportInk) {
        page.add(ReportText(value, x, top, size, bold, color))
    }
    fun right(value: String, edge: Float, top: Float, size: Float = 10f, bold: Boolean = false) {
        text(value, edge - measure(value, size, bold), top, size, bold)
    }
    fun newPage() {
        page = mutableListOf(); pages.add(page)
        text("Simple Menu", 36f, 30f, 22f, true, reportGreen)
        text("Ventas del día: $day", 36f, 61f, 11f)
        page.add(ReportBox(36f, 83f, 523f, 1f, reportGreen))
        y = 98f
        if (pages.size == 1) {
            page.add(ReportBox(36f, 98f, 523f, 58f, reportPale))
            text("TOTAL DEL DÍA", 48f, 108f, 9f, true)
            text(money(daily.sumOf { it.total }), 48f, 125f, 18f, true, reportGreen)
            right("${daily.size} pedidos / ${daily.sumOf { s -> s.lines.sumOf { it.quantity } }} artículos", 547f, 124f)
            y = 174f
        }
    }
    fun saleHeader(sale: Sale, continued: Boolean = false) {
        text("Pedido #${sale.id}${if (continued) " (continuación)" else ""}", 36f, y, 12f, true)
        y += 23f
        page.add(ReportBox(36f, y, 523f, 23f, reportPale))
        text("Artículo", 44f, y + 6, 9f, true)
        right("Cant.", 336f, y + 6, 9f, true)
        right("Precio unitario", 441f, y + 6, 9f, true)
        right("Importe", 551f, y + 6, 9f, true)
        y += 29f
    }
    newPage()
    if (daily.isEmpty()) text("No hay ventas registradas para esta fecha.", 36f, y)
    daily.forEach { sale ->
        if (y + 110 > 778f) newPage()
        saleHeader(sale)
        sale.lines.forEachIndexed { lineIndex, line ->
            val wrapped = wrapReportText(line.name, 255f) { measure(it, 10f, false) }.chunked(32)
            wrapped.forEachIndexed { partIndex, names ->
                val height = maxOf(28f, names.size * 14f + 10f)
                val last = lineIndex == sale.lines.lastIndex && partIndex == wrapped.lastIndex
                if (y + height + (if (last) 34f else 0f) > 778f) { newPage(); saleHeader(sale, true) }
                names.forEachIndexed { index, name -> text(name, 44f, y + 5f + index * 14f) }
                if (partIndex == 0) {
                    right(line.quantity.toString(), 336f, y + 5f)
                    right(money(line.cents), 441f, y + 5f)
                    right(money(line.total), 551f, y + 5f)
                }
                y += height
                page.add(ReportBox(36f, y, 523f, .5f, reportPale))
            }
        }
        y += 8f
        text("Total del pedido", 44f, y, 10f, true)
        right(money(sale.total), 551f, y, 11f, true)
        y += 34f
    }
    if (daily.isNotEmpty()) {
        if (y + 54f > 778f) newPage()
        page.add(ReportBox(36f, y, 523f, 48f, reportPale))
        text("TOTAL DEL DÍA", 48f, y + 16f, 12f, true, reportGreen)
        right(money(daily.sumOf { it.total }), 547f, y + 13f, 18f, true)
    }
    pages.forEachIndexed { index, commands ->
        commands.add(ReportBox(36f, 799f, 523f, .5f, reportGreen))
        commands.add(ReportText("Simple Menu / $day", 36f, 810f, 9f))
        val footer = "Página ${index + 1} de ${pages.size}"
        commands.add(ReportText(footer, 559f - measure(footer, 9f, false), 810f, 9f))
    }
    return pages
}
