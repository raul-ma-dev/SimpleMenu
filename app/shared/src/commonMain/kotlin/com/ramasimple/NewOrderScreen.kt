package com.ramasimple

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NewOrderScreen(
    products: List<Product>,
    cart: Map<Long, Int>,
    enabled: Boolean,
    error: String?,
    notice: String?,
    onQuantityChange: (Product, Int) -> Unit,
    onCloseOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOrder by remember { mutableStateOf(false) }
    val selected = products.filter { (cart[it.id] ?: 0) > 0 }
    val quantity = selected.sumOf { cart[it.id] ?: 0 }
    val total = selected.sumOf { it.cents * (cart[it.id] ?: 0) }

    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // The header and order summary never belong to the product scroll container.
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Simple Menu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Nuevo pedido", style = MaterialTheme.typography.titleMedium)
                Text("Elige productos y ajusta sus cantidades.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Card(onClick = { showOrder = true }, modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pedido actual", style = MaterialTheme.typography.titleSmall)
                    Text("$quantity artículos", style = MaterialTheme.typography.bodySmall)
                    Text(money(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Ver detalle", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Button(onClick = onCloseOrder, enabled = selected.isNotEmpty() && enabled, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Cerrar pedido")
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        ProductBrowser(products, categoryButtons = true) { filtered ->
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (products.isEmpty()) Text("Primero agrega productos en la sección Productos.")
                OrderProductGrid(filtered, cart, onQuantityChange = onQuantityChange)
            }
        }
    }

    // A long order is inspected explicitly without making the fixed header grow.
    if (showOrder) AlertDialog(
        onDismissRequest = { showOrder = false },
        title = { Text("Pedido actual") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selected.isEmpty()) Text("Aún no has agregado productos.")
                OrderProductGrid(selected, cart, compact = true, onQuantityChange = onQuantityChange)
            }
        },
        confirmButton = { TextButton(onClick = { showOrder = false }) { Text("Volver al pedido") } },
    )
}
