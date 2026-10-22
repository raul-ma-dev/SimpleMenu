package com.ramasimple

import androidx.compose.runtime.*
import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberShareFileLauncher
import kotlinx.coroutines.*
import kotlin.uuid.Uuid

data class DailyReportActions(
    val busy: Boolean,
    val message: String?,
    val error: String?,
    val export: (String, List<Sale>) -> Unit,
    val share: (String, List<Sale>) -> Unit,
)

// Native launchers stay in the root composition on both Android and iOS.
@Composable
fun rememberDailyReportActions(): DailyReportActions {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    val saver = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onError = { error = "No se pudo abrir el destino de exportación."; pending = null; busy = false },
        onResult = { file ->
            val bytes = pending
            pending = null
            if (file == null || bytes == null) busy = false
            else scope.launch {
                try {
                    withContext(Dispatchers.IO) { file.write(bytes) }
                    message = "Reporte guardado correctamente."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) { error = "No se pudo guardar el reporte. Intenta de nuevo." }
                finally { busy = false }
            }
        },
    )
    val sharer = rememberShareFileLauncher(onError = { error = "No se pudo abrir el menú para compartir. Puedes exportar el reporte y compartirlo desde Archivos." })
    return DailyReportActions(
        busy, message, error,
        export = { day, sales ->
            if (!busy) {
                busy = true; error = null; message = null
                scope.launch {
                    try {
                        // Capture this date and sales before opening the native dialog.
                        pending = withContext(Dispatchers.Default) { dailySalesCsv(day, sales).encodeToByteArray() }
                        saver.launch(suggestedName = "ventas-$day", defaultExtension = "csv")
                    } catch (cancelled: CancellationException) {
                        pending = null; busy = false; throw cancelled
                    } catch (_: Exception) { pending = null; busy = false; error = "No se pudo preparar el reporte." }
                }
            }
        },
        share = { day, sales ->
            if (!busy) {
                busy = true; error = null; message = null
                scope.launch {
                    try {
                        val file = withContext(Dispatchers.IO) {
                            // Leave the cache copy available while the receiving app reads it.
                            val directory = FileKit.cacheDir / "sales-export-${Uuid.random()}"
                            directory.createDirectories()
                            val report = directory / "ventas-$day.csv"
                            report.write(dailySalesCsv(day, sales).encodeToByteArray())
                            report
                        }
                        sharer.launch(file)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) { error = "No se pudo preparar el reporte para compartir." }
                    finally { busy = false }
                }
            }
        },
    )
}
