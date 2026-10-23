package com.rama.simplemenu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class HsvColor(val hue: Float, val saturation: Float, val brightness: Float)
internal fun colorToHsv(color: Color): HsvColor {
    val high = max(color.red, max(color.green, color.blue))
    val low = min(color.red, min(color.green, color.blue))
    val delta = high - low
    val sector = when {
        delta == 0f -> 0f
        high == color.red -> (color.green - color.blue) / delta
        high == color.green -> (color.blue - color.red) / delta + 2f
        else -> (color.red - color.green) / delta + 4f
    }
    return HsvColor((sector * 60f + 360f) % 360f, if (high == 0f) 0f else delta / high, high)
}

@Composable
fun CustomColorDialog(label: String, initialColor: Long, onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    val initial = remember { colorToHsv(appearanceColor(initialColor)) }
    var hue by remember { mutableStateOf(initial.hue) }
    var saturation by remember { mutableStateOf(initial.saturation) }
    var brightness by remember { mutableStateOf(initial.brightness) }
    val color = Color.hsv(hue, saturation, brightness)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir color: $label") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(Modifier.fillMaxWidth().height(56.dp), color = color, contentColor = foregroundColor(color), shape = MaterialTheme.shapes.medium) {
                    Box(contentAlignment = Alignment.Center) { Text(hexColor(color.rgbValue())) }
                }
                Text("Toca o arrastra para ajustar el color.", style = MaterialTheme.typography.bodySmall)
                SaturationBrightnessSquare(hue, saturation, brightness) { s, b -> saturation = s; brightness = b }
                ColorSlider("Tono", hue, 0f..360f, listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red), "${hue.roundToInt()}°") { hue = it }
                ColorSlider("Saturación", saturation, 0f..1f, listOf(Color.hsv(hue, 0f, brightness), Color.hsv(hue, 1f, brightness)), "${(saturation * 100).roundToInt()} %") { saturation = it }
                ColorSlider("Brillo", brightness, 0f..1f, listOf(Color.Black, Color.hsv(hue, saturation, 1f)), "${(brightness * 100).roundToInt()} %") { brightness = it }
            }
        },
        confirmButton = { TextButton(onClick = { onSelect(color.rgbValue()) }) { Text("Usar color") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun SaturationBrightnessSquare(hue: Float, saturation: Float, brightness: Float, onChange: (Float, Float) -> Unit) {
    val currentOnChange by rememberUpdatedState(onChange)
    Canvas(Modifier.fillMaxWidth().height(160.dp)
        .pointerInput(Unit) { detectTapGestures { offset -> currentOnChange((offset.x / size.width).coerceIn(0f, 1f), (1f - offset.y / size.height).coerceIn(0f, 1f)) } }
        .pointerInput(Unit) {
            detectDragGestures(onDragStart = { offset -> currentOnChange((offset.x / size.width).coerceIn(0f, 1f), (1f - offset.y / size.height).coerceIn(0f, 1f)) }) { change, _ ->
                change.consume()
                currentOnChange((change.position.x / size.width).coerceIn(0f, 1f), (1f - change.position.y / size.height).coerceIn(0f, 1f))
            }
        }) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val point = Offset(saturation * size.width, (1f - brightness) * size.height)
        drawCircle(Color.Black, 8.dp.toPx(), point, style = Stroke(3.dp.toPx()))
        drawCircle(Color.White, 8.dp.toPx(), point, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, colors: List<Color>, valueLabel: String, onChange: (Float) -> Unit) {
    Column {
        Text("$label: $valueLabel", style = MaterialTheme.typography.labelLarge)
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(8.dp).background(Brush.horizontalGradient(colors), MaterialTheme.shapes.small))
            Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.semantics { contentDescription = label }, colors = SliderDefaults.colors(activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
        }
    }
}
