package com.ramasimple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun BusinessHeader(appearance: AppAppearance, compact: Boolean = false) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (appearance.logo != null) BusinessLogo(appearance, Modifier.size(48.dp))
            Text(appearance.businessName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (appearance.logo != null) BusinessLogo(appearance, Modifier.size(64.dp))
            Text(appearance.businessName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BusinessLogo(appearance: AppAppearance, modifier: Modifier) {
    ProductPhoto(appearance.logo, appearance.businessName, modifier, ContentScale.Fit, "Logo de ${appearance.businessName}")
}

@Composable
fun AppearanceSection(
    saved: AppAppearance,
    logoEditor: ProductPhotoEditor,
    pickLogo: () -> Unit,
    enabled: Boolean,
    onSave: (AppAppearance) -> Boolean,
) {
    var name by remember(saved) { mutableStateOf(saved.businessName) }
    var primary by remember(saved) { mutableStateOf(hexColor(saved.primary)) }
    var secondary by remember(saved) { mutableStateOf(hexColor(saved.secondary)) }
    var background by remember(saved) { mutableStateOf(hexColor(saved.background)) }
    var cardBackground by remember(saved) { mutableStateOf(hexColor(cardBackgroundColor(saved).rgbValue())) }
    var textColor by remember(saved) { mutableStateOf(hexColor(preferredTextColor(saved).rgbValue())) }
    var navigationBackground by remember(saved) { mutableStateOf(hexColor(navigationBackgroundColor(saved).rgbValue())) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val draft = AppAppearance(
        name.trim().ifBlank { "Simple Menu" }, logoEditor.photo,
        parseHexColor(primary) ?: saved.primary,
        parseHexColor(secondary) ?: saved.secondary,
        parseHexColor(background) ?: saved.background,
        parseHexColor(cardBackground) ?: saved.cardBackground,
        parseHexColor(textColor) ?: saved.textColor,
        parseHexColor(navigationBackground) ?: saved.navigationBackground,
    )
    val valid = name.isNotBlank() && listOf(primary, secondary, background, cardBackground, textColor, navigationBackground).all { parseHexColor(it) != null }
    Text("Personalización", style = MaterialTheme.typography.headlineSmall)
    Text("Adapta la app a tu negocio. Los cambios se aplican al guardar.")
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Identidad del negocio", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = name, onValueChange = { name = it.take(40); feedback = null }, label = { Text("Nombre de la empresa") }, isError = name.isBlank(), supportingText = { if (name.isBlank()) Text("Escribe el nombre de tu negocio.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (logoEditor.photo != null) ProductPhoto(logoEditor.photo, draft.businessName, Modifier.fillMaxWidth().height(130.dp), ContentScale.Fit, "Vista previa del logo")
            else Text("Agrega el logo de tu empresa para mostrarlo en la cabecera.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { feedback = null; pickLogo() }, enabled = enabled && !logoEditor.busy) { Text(if (logoEditor.photo == null) "Agregar logo" else "Cambiar logo") }
                if (logoEditor.photo != null) TextButton(onClick = { logoEditor.remove(); feedback = null }, enabled = !logoEditor.busy) { Text("Quitar logo") }
            }
            Text("Elige una imagen de hasta 20 MB. Se conserva la transparencia de los logos PNG.", style = MaterialTheme.typography.bodySmall)
            if (logoEditor.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Preparando logo…") }
            logoEditor.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Colores de la app", style = MaterialTheme.typography.titleMedium)
            AppearanceColorInput("Color principal", primary) { primary = it; feedback = null }
            AppearanceColorInput("Color secundario", secondary) { secondary = it; feedback = null }
            AppearanceColorInput("Color de fondo", background, backgrounds = true) { background = it; feedback = null }
            AppearanceColorInput("Fondo de las tarjetas", cardBackground, backgrounds = true) { cardBackground = it; feedback = null }
            AppearanceColorInput("Color de los textos", textColor) { textColor = it; feedback = null }
            AppearanceColorInput("Barra de navegación", navigationBackground, backgrounds = true) { navigationBackground = it; feedback = null }
            Text("Puedes elegir un tono o escribir un código #RRGGBB. Los tonos se adaptan para mantener el texto legible.", style = MaterialTheme.typography.bodySmall)
        }
    }
    Text("Vista previa", style = MaterialTheme.typography.titleMedium)
    CompositionLocalProvider(LocalAppAppearance provides draft) {
    MaterialTheme(colorScheme = appearanceColorScheme(draft)) {
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.background, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BusinessHeader(draft)
                Text("Así se verá tu negocio", color = MaterialTheme.colorScheme.secondary)
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Producto de ejemplo", fontWeight = FontWeight.Bold)
                        Text("Así se verán los textos en tus tarjetas.")
                        Text("Total ${money(2500)}")
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text("Nuevo pedido") }
                    FilterChip(selected = true, onClick = {}, label = { Text("Categoría") })
                }
                AppNavigationBar(draft, selected = 0, preview = true, onSelect = {})
            }
        }
    }
    }
    feedback?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(onClick = {
        if (onSave(draft)) { feedback = "Personalización guardada."; saveError = null }
        else saveError = "No se pudo guardar. Tus cambios siguen disponibles para volver a intentar."
    }, enabled = enabled && valid && !logoEditor.busy, modifier = Modifier.fillMaxWidth()) { Text("Guardar personalización") }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = {
            name = saved.businessName; primary = hexColor(saved.primary); secondary = hexColor(saved.secondary); background = hexColor(saved.background)
            cardBackground = hexColor(cardBackgroundColor(saved).rgbValue()); textColor = hexColor(preferredTextColor(saved).rgbValue()); navigationBackground = hexColor(navigationBackgroundColor(saved).rgbValue())
            logoEditor.discard(); logoEditor.open(saved.logo); feedback = null; saveError = null
        }, enabled = !logoEditor.busy) { Text("Descartar cambios") }
        TextButton(onClick = {
            val defaults = AppAppearance()
            name = defaults.businessName; primary = hexColor(defaults.primary); secondary = hexColor(defaults.secondary); background = hexColor(defaults.background)
            cardBackground = hexColor(cardBackgroundColor(defaults).rgbValue()); textColor = hexColor(preferredTextColor(defaults).rgbValue()); navigationBackground = hexColor(navigationBackgroundColor(defaults).rgbValue())
            logoEditor.remove(); feedback = "Pulsa Guardar para restablecer la apariencia."; saveError = null
        }, enabled = !logoEditor.busy) { Text("Restablecer apariencia") }
    }
}

@Composable
private fun AppearanceColorInput(label: String, value: String, backgrounds: Boolean = false, onChange: (String) -> Unit) {
    val palette = if (backgrounds) listOf(0xF7F9F6L, 0xFFFFFFL, 0xFFF5EAL, 0xEFF6FFL, 0xF5F0FFL, 0x18211EL)
    else listOf(0x146C59L, 0x1565C0L, 0x6A1B9AL, 0xB3261EL, 0x9C3D10L, 0x55645DL, 0x222222L, 0xFFFFFFL)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(value = value, onValueChange = { onChange(it.take(7)) }, label = { Text(label) }, placeholder = { Text("#146C59") }, isError = parseHexColor(value) == null,
            supportingText = { if (parseHexColor(value) == null) Text("Escribe seis dígitos hexadecimales, por ejemplo #146C59.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), singleLine = true, modifier = Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            palette.forEach { color ->
                FilterChip(selected = parseHexColor(value) == color, onClick = { onChange(hexColor(color)) },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "$label ${hexColor(color)}" },
                    label = { Box(Modifier.size(24.dp).background(appearanceColor(color), CircleShape)) })
            }
        }
    }
}
