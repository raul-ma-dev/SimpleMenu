package com.ramasimple

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class DailyPdfDeviceTest {
    @Test fun generatesAndRendersPaginatedDailyReport() {
        val day = "2026-10-21"
        val sales = listOf(
            Sale(1, day, listOf(SaleLine("Café de olla", 2500, 2), SaleLine("Pan dulce - promoción especial", 1550, 3))),
            Sale(2, day, List(45) { SaleLine("Artículo ${it + 1}: desayuno especial con café, pan y jugo de naranja", 12550, (it % 5) + 1) }),
            Sale(3, day, listOf(SaleLine("Chocolate frío ☕", 4200, 2), SaleLine("ProductoConNombreMuyLargo".repeat(8), 999999999, 999))),
            Sale(4, "2026-10-22", listOf(SaleLine("No incluir", 500, 1))),
        )
        val bytes = dailySalesPdf(day, sales)
        assertTrue(bytes.take(5).toByteArray().decodeToString().startsWith("%PDF-"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "daily-pdf-test.pdf")
        file.writeBytes(bytes)
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertTrue(renderer.pageCount >= 3)
            for (index in 0 until renderer.pageCount) renderer.openPage(index).use { page ->
                assertEquals(REPORT_WIDTH, page.width)
                assertEquals(REPORT_HEIGHT, page.height)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                try { page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                finally { bitmap.recycle() }
            }
        }
        Log.i("PDF_SAMPLE_PATH", file.absolutePath)
    }
}
