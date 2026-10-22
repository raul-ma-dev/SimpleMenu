package com.ramasimple

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.*
import kotlin.uuid.Uuid

// Store only a relative filename: the iOS sandbox location can change between launches.
private fun photoFile(reference: String): PlatformFile {
    require(Regex("product-[a-f0-9-]+\\.jpg").matches(reference))
    return PlatformFile(FileKit.filesDir, reference)
}

suspend fun deleteProductPhoto(reference: String?) {
    if (reference != null) withContext(Dispatchers.IO) {
        runCatching { photoFile(reference).delete(mustExist = false) }
    }
}

class ProductPhotoEditor(private val scope: CoroutineScope) {
    var photo by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var original: String? = null
    private var generation = 0

    fun open(reference: String?) {
        generation++
        original = reference
        photo = reference
        error = null
    }
    fun discard() {
        generation++
        val unused = photo.takeIf { it != original }
        photo = original
        scope.launch { deleteProductPhoto(unused) }
    }
    fun commit() {
        val unused = original.takeIf { it != photo }
        original = photo
        scope.launch { deleteProductPhoto(unused) }
    }
    fun remove() {
        val unused = photo.takeIf { it != original }
        photo = null
        error = null
        scope.launch { deleteProductPhoto(unused) }
    }
    fun pickerFailed() { error = "No se pudo abrir la galería. Intenta de nuevo." }
    fun import(file: PlatformFile) {
        val session = generation
        busy = true
        error = null
        scope.launch {
            var created: String? = null
            try {
                val reference = withContext(Dispatchers.IO) {
                    require(file.size() <= 20 * 1024 * 1024) { "La foto debe pesar menos de 20 MB." }
                    val bytes = file.readBytes()
                    require(bytes.size <= 20 * 1024 * 1024) { "La foto debe pesar menos de 20 MB." }
                    val compressed = FileKit.compressImage(bytes, quality = 85, maxWidth = 640, maxHeight = 640, imageFormat = ImageFormat.JPEG)
                    val name = "product-${Uuid.random()}.jpg"
                    created = name
                    photoFile(name).write(compressed)
                    name
                }
                if (session == generation) {
                    val unused = photo.takeIf { it != original }
                    photo = reference
                    created = null
                    deleteProductPhoto(unused)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (session == generation) error = "No se pudo cargar la foto. Elige una imagen de hasta 20 MB."
            } finally {
                withContext(NonCancellable) { deleteProductPhoto(created) }
                busy = false
            }
        }
    }
}

// Keep the native picker in the root composition, outside the product dialog (iOS).
@Composable
fun rememberProductPhotoPicker(editor: ProductPhotoEditor, active: Boolean): () -> Unit {
    val picker = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onError = { editor.pickerFailed() },
        onResult = { file -> if (active && file != null) editor.import(file) },
    )
    return { picker.launch() }
}

@Composable
fun ProductPhoto(reference: String?, name: String, modifier: Modifier = Modifier) {
    var bitmap by remember(reference) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(reference) { mutableStateOf(false) }
    LaunchedEffect(reference) {
        if (reference != null) {
            try {
                bitmap = withContext(Dispatchers.IO) { photoFile(reference).toImageBitmap() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) { failed = true }
        }
    }
    val image = bitmap
    if (image != null) Image(image, "Foto de $name", modifier.clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
    else Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(Modifier.padding(12.dp)) { Text(if (failed) "Foto no disponible" else if (reference == null) "Sin foto" else "Cargando foto…", style = MaterialTheme.typography.bodySmall) }
    }
}
