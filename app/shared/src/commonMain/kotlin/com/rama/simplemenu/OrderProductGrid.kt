package com.rama.simplemenu

import androidx.compose.foundation.BorderStroke
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
fun OrderProductGrid(
    products: List<Product>,
    cart: Map<Long, Int>,
    compact: Boolean = false,
    onQuantityChange: (Product, Int) -> Unit,
) {
    AdaptiveProductGrid(products) { product, modifier ->
        val quantity = cart[product.id] ?: 0
        AppCard(modifier, border = if (quantity > 0) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
            if (!compact) ProductPhoto(product.photo, product.name, Modifier.fillMaxWidth().aspectRatio(1.3f))
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, fontWeight = FontWeight.Bold, maxLines = 1, minLines = 1, overflow = TextOverflow.Ellipsis)
                if (!compact) Text(product.categoryLabel(), style = MaterialTheme.typography.bodySmall, maxLines = 1, minLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        money(product.cents),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(
                        onClick = { onQuantityChange(product, quantity - 1) },
                        enabled = quantity > 0,
                        modifier = Modifier.width(28.dp).height(48.dp).semantics { contentDescription = "Quitar una unidad de ${product.name}" },
                        contentPadding = PaddingValues(0.dp),
                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                        Text("$quantity", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = "$quantity unidades de ${product.name}" })
                    }
                    TextButton(
                        onClick = { onQuantityChange(product, quantity + 1) },
                        enabled = quantity < 999,
                        modifier = Modifier.width(28.dp).height(48.dp).semantics { contentDescription = "Agregar una unidad de ${product.name}" },
                        contentPadding = PaddingValues(0.dp),
                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }
                Text("Subtotal: ${money(product.cents * quantity)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(name = "Nuevo pedido - celular", showBackground = true, widthDp = 390)
@Preview(name = "Nuevo pedido - tablet", showBackground = true, widthDp = 840)
@Composable
fun OrderProductGridPreview() {
    OrderGridPreviewContent(compact = false)
}

@Preview(name = "Pedido actual - compacto", showBackground = true, widthDp = 390)
@Composable
fun OrderProductGridCompactPreview() {
    OrderGridPreviewContent(compact = true)
}

@Composable
private fun OrderGridPreviewContent(compact: Boolean) {
    val products = listOf(
        Product(1, "Café de olla", 2500, "Bebidas", "Calientes"),
        Product(2, "Pan dulce", 1800, "Alimentos", "Panadería"),
        Product(3, "Jugo de naranja natural", 3500, "Bebidas", "Frías"),
    )
    var cart by remember { mutableStateOf(mapOf(1L to 2, 2L to 1)) }
    MaterialTheme(colorScheme = appearanceColorScheme(AppAppearance())) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxWidth().padding(20.dp)) {
                OrderProductGrid(
                    products = if (compact) products.filter { (cart[it.id] ?: 0) > 0 } else products,
                    cart = cart,
                    compact = compact,
                    onQuantityChange = { product, quantity ->
                        cart = if (quantity <= 0) cart - product.id else cart + (product.id to quantity)
                    },
                )
            }
        }
    }
}
