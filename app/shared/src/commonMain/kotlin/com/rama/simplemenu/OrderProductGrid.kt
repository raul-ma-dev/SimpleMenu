package com.rama.simplemenu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, fontWeight = FontWeight.Bold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                if (!compact) Text(product.categoryLabel(), style = MaterialTheme.typography.bodySmall, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                Text(money(product.cents), fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onQuantityChange(product, quantity - 1) },
                        enabled = quantity > 0,
                        modifier = Modifier.weight(1f).height(48.dp).semantics { contentDescription = "Quitar una unidad de ${product.name}" },
                        contentPadding = PaddingValues(0.dp),
                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("$quantity", fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = "$quantity unidades de ${product.name}" })
                    }
                    TextButton(
                        onClick = { onQuantityChange(product, quantity + 1) },
                        enabled = quantity < 999,
                        modifier = Modifier.weight(1f).height(48.dp).semantics { contentDescription = "Agregar una unidad de ${product.name}" },
                        contentPadding = PaddingValues(0.dp),
                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }
                Text("Subtotal: ${money(product.cents * quantity)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
