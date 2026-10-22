package com.ramasimple

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SalesSection(
    shop: ShopData,
    today: String,
    selectedDay: String,
    onDayChange: (String) -> Unit,
    onSave: (ShopData) -> Boolean,
    report: DailyReportActions,
) {
    var editing by remember { mutableStateOf<Sale?>(null) }
    var deleting by remember { mutableStateOf<Sale?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var format by remember { mutableStateOf(DailyReportFormat.CSV) }
    Text("Ventas por día", style = MaterialTheme.typography.headlineSmall)
    val days = (shop.sales.map { it.day } + today + selectedDay).distinct().sortedDescending()
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("Fecha: $selectedDay ▾") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            days.forEach { day -> DropdownMenuItem(text = { Text(day) }, onClick = { onDayChange(day); expanded = false; feedback = null }) }
        }
    }
    val sales = shop.sales.filter { it.day == selectedDay }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Total del día")
            Text(money(sales.sumOf { it.total }), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("${sales.size} pedidos · ${sales.sumOf { s -> s.lines.sumOf { it.quantity } }} artículos")
            Text("Formato del reporte", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DailyReportFormat.entries.forEach { option ->
                    FilterChip(selected = format == option, onClick = { format = option }, enabled = !report.busy, label = { Text(option.name) })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { report.export(selectedDay, sales, format) }, enabled = !report.busy && sales.isNotEmpty()) { Text("Exportar día (${format.name})") }
                OutlinedButton(onClick = { report.share(selectedDay, sales, format) }, enabled = !report.busy && sales.isNotEmpty()) { Text("Compartir ${format.name}") }
            }
            if (report.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            report.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            report.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    feedback?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    if (sales.isEmpty()) Text("Todavía no hay ventas registradas en esta fecha.")
    sales.asReversed().forEach { sale ->
        key(sale.id) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pedido #${sale.id}", fontWeight = FontWeight.Bold)
                    sale.lines.forEach { Text("${it.quantity} × ${it.name} · ${money(it.cents)} = ${money(it.total)}") }
                    HorizontalDivider()
                    Text("Total ${money(sale.total)}", fontWeight = FontWeight.Bold)
                    Row {
                        TextButton(onClick = { editing = sale }) { Text("Editar") }
                        TextButton(onClick = { deleting = sale; deleteError = null }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    editing?.let { sale ->
        SaleEditorDialog(
            sale = sale,
            products = shop.products,
            onDismiss = { editing = null },
            onSave = { changed ->
                val saved = onSave(shop.updateSale(changed))
                if (saved) { editing = null; feedback = "Pedido #${changed.id} actualizado." }
                saved
            },
        )
    }
    deleting?.let { sale ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar pedido #${sale.id}") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se eliminará la venta de ${money(sale.total)} del $selectedDay y se actualizará el total del día. Esta acción no se puede deshacer.")
                deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton(onClick = {
                if (onSave(shop.deleteSale(sale.id))) { deleting = null; feedback = "Pedido #${sale.id} eliminado." }
                else deleteError = "No se pudo eliminar. Intenta de nuevo."
            }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SaleEditorDialog(sale: Sale, products: List<Product>, onDismiss: () -> Unit, onSave: (Sale) -> Boolean) {
    val drafts = remember(sale.id) { mutableStateListOf<SaleLineDraft>().apply { addAll(sale.lines.map { it.toDraft() }) } }
    var catalogExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val parsed = drafts.map { it.toSaleLine() }
    val valid = parsed.isNotEmpty() && parsed.all { it != null }
    val total = parsed.filterNotNull().sumOf { it.total }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar pedido #${sale.id}") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Fecha: ${sale.day}")
                Text("Corrige los artículos, precios o cantidades de esta venta.", style = MaterialTheme.typography.bodySmall)
                drafts.forEachIndexed { index, draft ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = draft.name, onValueChange = { drafts[index] = draft.copy(name = it.take(80)) }, label = { Text("Artículo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = draft.price, onValueChange = { drafts[index] = draft.copy(price = it) }, label = { Text("Precio unitario ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = draft.quantity, onValueChange = { drafts[index] = draft.copy(quantity = it.take(4)) }, label = { Text("Cantidad (1–999)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                            draft.toSaleLine()?.let { Text("Subtotal ${money(it.total)}") }
                            TextButton(onClick = { drafts.removeAt(index) }) { Text("Quitar artículo") }
                        }
                    }
                }
                if (products.isNotEmpty()) Box {
                    OutlinedButton(onClick = { catalogExpanded = true }) { Text("Agregar del catálogo") }
                    DropdownMenu(expanded = catalogExpanded, onDismissRequest = { catalogExpanded = false }) {
                        products.forEach { product ->
                            DropdownMenuItem(text = { Text("${product.name} · ${money(product.cents)}") }, onClick = {
                                drafts.add(SaleLine(product.name, product.cents, 1).toDraft())
                                catalogExpanded = false
                            })
                        }
                    }
                }
                TextButton(onClick = { drafts.add(SaleLineDraft()) }) { Text("Agregar otro artículo") }
                if (!valid) Text("Agrega al menos un artículo con nombre, precio mayor a cero y cantidad entre 1 y 999.", color = MaterialTheme.colorScheme.error)
                Text("Total actualizado: ${money(total)}", fontWeight = FontWeight.Bold)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = {
            if (!onSave(sale.copy(lines = parsed.filterNotNull()))) error = "No se pudo guardar. Tus cambios siguen aquí para volver a intentar."
        }) { Text("Guardar cambios") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
