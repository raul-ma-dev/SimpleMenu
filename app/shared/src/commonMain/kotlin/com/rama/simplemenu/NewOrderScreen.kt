package com.rama.simplemenu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NewOrderScreen(
    appearance: AppAppearance,
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
                BusinessHeader(appearance, compact = true)
                Text("Nuevo pedido", style = MaterialTheme.typography.titleMedium)
                Text("Elige productos y ajusta sus cantidades.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            AppCard(onClick = { showOrder = true }, modifier = Modifier.weight(1f)) {
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (selected.isEmpty()) Text("Aún no has agregado productos.")
                    selected.forEach { product ->
                        key(product.id) {
                            val units = cart[product.id] ?: 0
                            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(product.name, fontWeight = FontWeight.Bold)
                                Text("${money(product.cents)} por unidad", style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { onQuantityChange(product, units - 1) },
                                        enabled = enabled && units > 0,
                                        modifier = Modifier.size(48.dp).semantics { contentDescription = "Quitar una unidad de ${product.name}" },
                                        contentPadding = PaddingValues(0.dp),
                                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                                    Text("$units", modifier = Modifier.widthIn(min = 24.dp).semantics { contentDescription = "$units unidades de ${product.name}" }, fontWeight = FontWeight.Bold)
                                    TextButton(
                                        onClick = { onQuantityChange(product, units + 1) },
                                        enabled = enabled && units < 999,
                                        modifier = Modifier.size(48.dp).semantics { contentDescription = "Agregar una unidad de ${product.name}" },
                                        contentPadding = PaddingValues(0.dp),
                                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text("Subtotal", style = MaterialTheme.typography.labelSmall)
                                        Text(money(product.cents * units), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                if (selected.isNotEmpty()) {
                    Text("$quantity artículos · Total ${money(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = { TextButton(onClick = { showOrder = false }) { Text("Volver al pedido") } },
    )
}

@Preview(name = "Nuevo pedido - celular", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Nuevo pedido - tablet", showBackground = true, widthDp = 840, heightDp = 900)
@Composable
fun NewOrderScreenPreview() {
    val appearance = AppAppearance(businessName = "Café de ejemplo")
    val products = listOf(
        Product(1, "Café de olla", 2500, "Bebidas", "Calientes"),
        Product(2, "Pan dulce", 1800, "Alimentos", "Panadería"),
        Product(3, "Jugo de naranja", 3500, "Bebidas", "Frías"),
        Product(4, "Sándwich", 5500, "Alimentos", "Preparados"),
    )
    var cart by remember { mutableStateOf(mapOf(1L to 2, 2L to 1)) }
    var notice by remember { mutableStateOf<String?>(null) }
    CompositionLocalProvider(LocalAppAppearance provides appearance) {
        MaterialTheme(colorScheme = appearanceColorScheme(appearance)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                NewOrderScreen(
                    appearance = appearance,
                    products = products,
                    cart = cart,
                    enabled = true,
                    error = null,
                    notice = notice,
                    onQuantityChange = { product, quantity ->
                        cart = if (quantity <= 0) cart - product.id else cart + (product.id to quantity.coerceAtMost(999))
                        notice = null
                    },
                    onCloseOrder = { cart = emptyMap(); notice = "Pedido de ejemplo cerrado." },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
