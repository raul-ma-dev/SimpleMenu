package com.rama.simplemenu

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.vinceglb.filekit.*
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class LogoDeviceTest {
    @Test fun logoKeepsTransparencyAndOnlyDeletesTheOldFileAfterCommit() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "transparent-logo-test.png")
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        bitmap.setPixel(8, 8, Color.RED)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val editor = ProductPhotoEditor(scope, logo = true)
        var reference: String? = null
        try {
            withContext(Dispatchers.Main) { editor.open(null); editor.import(PlatformFile(source.absolutePath)) }
            withTimeout(10000) { while (editor.busy) delay(20) }
            assertNull(editor.error)
            reference = assertNotNull(editor.photo)
            assertTrue(reference.startsWith("logo-") && reference.endsWith(".png"))
            val imported = FileKit.filesDir / reference
            val bytes = imported.readBytes()
            val result = assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            try { assertEquals(0, Color.alpha(result.getPixel(0, 0))) }
            finally { result.recycle() }
            withContext(Dispatchers.Main) { editor.commit(); editor.remove() }
            assertTrue(imported.exists(), "Removing a saved logo in the draft must keep the saved file")
            withContext(Dispatchers.Main) { editor.discard(); editor.open(reference) }
            assertEquals(reference, editor.photo)
            withContext(Dispatchers.Main) { editor.remove(); editor.commit() }
            withTimeout(10000) { while (imported.exists()) delay(20) }
        } finally {
            scope.cancel()
            deleteProductPhoto(reference)
            source.delete()
        }
    }
}
