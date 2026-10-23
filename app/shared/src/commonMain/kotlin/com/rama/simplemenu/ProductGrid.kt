package com.rama.simplemenu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ProductGrid(
    products: List<Product>,
    cart: Map<Long, Int>,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
) {
    AdaptiveProductGrid(products) { product, modifier ->
        ProductTile(
            product = product,
            inCart = cart[product.id] != null,
            onEdit = { onEdit(product) },
            onDelete = { onDelete(product) },
            modifier = modifier,
        )
    }
}

@Preview(name = "Productos - celular", showBackground = true, widthDp = 390)
@Preview(name = "Productos - tablet", showBackground = true, widthDp = 840)
@Composable
fun ProductGridPreview() {
    val appearance = AppAppearance()
    MaterialTheme(colorScheme = appearanceColorScheme(appearance)) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxWidth().padding(20.dp)) {
                ProductGrid(
                    products = listOf(
                        Product(1, "Café de olla", 2500, "Bebidas", "Calientes"),
                        Product(2, "Pan dulce", 1800, "Alimentos", "Panadería"),
                        Product(3, "Jugo de naranja natural", 3500, "Bebidas", "Frías"),
                    ),
                    cart = mapOf(1L to 2),
                    onEdit = {},
                    onDelete = {},
                )
            }
        }
    }
}

@Composable
fun AdaptiveProductGrid(products: List<Product>, content: @Composable (Product, Modifier) -> Unit) {
    // The screen already scrolls vertically; keep a single scroll container.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 12.dp
        val columns = ((maxWidth + gap) / (172.dp + gap)).toInt().coerceAtLeast(2)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            products.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { product ->
                        key(product.id) {
                            content(product, Modifier.weight(1f))
                        }
                    }
                    // An incomplete row keeps the same card width as the other rows.
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ProductTile(
    product: Product,
    inCart: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    AppCard(modifier) {
        ProductPhoto(product.photo, product.name, Modifier.fillMaxWidth().aspectRatio(1.3f))
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(product.name, fontWeight = FontWeight.Bold, maxLines = 1, minLines = 1, overflow = TextOverflow.Ellipsis)
            Text(product.categoryLabel(), style = MaterialTheme.typography.bodySmall, maxLines = 1, minLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(money(product.cents), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Box {
                    TextButton(
                        onClick = { expanded = true },
                        modifier = Modifier.size(48.dp).semantics { contentDescription = "Opciones de ${product.name}" },
                        contentPadding = PaddingValues(0.dp),
                    ) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Editar") }, enabled = !inCart, onClick = { expanded = false; onEdit() })
                        DropdownMenuItem(text = { Text("Eliminar") }, enabled = !inCart, onClick = { expanded = false; onDelete() })
                    }
                }
            }
            if (inCart) Text("En el pedido actual", style = MaterialTheme.typography.bodySmall)
        }
    }
}
