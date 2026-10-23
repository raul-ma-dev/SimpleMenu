package com.rama.simplemenu

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

actual fun dailySalesPdf(day: String, sales: List<Sale>, businessName: String): ByteArray {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun font(size: Float, bold: Boolean) {
        paint.textSize = size
        paint.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    val pages = dailyPdfPages(day, sales, measure = { text, size, bold -> font(size, bold); paint.measureText(text) }, businessName = businessName)
    val document = PdfDocument()
    try {
        pages.forEachIndexed { index, commands ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(REPORT_WIDTH, REPORT_HEIGHT, index + 1).create())
            try {
                commands.forEach { command ->
                    val color = when (command) { is ReportText -> command.color; is ReportBox -> command.color }
                    paint.color = Color.rgb((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                    when (command) {
                        is ReportText -> { font(command.size, command.bold); page.canvas.drawText(command.value, command.x, command.y - paint.fontMetrics.ascent, paint) }
                        is ReportBox -> page.canvas.drawRect(command.x, command.y, command.x + command.width, command.y + command.height, paint)
                    }
                }
            } finally { document.finishPage(page) }
        }
        return ByteArrayOutputStream().use { output -> document.writeTo(output); output.toByteArray() }
    } finally { document.close() }
}
