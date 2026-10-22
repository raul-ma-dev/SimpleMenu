package com.ramasimple

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
private fun CategoryMenu(label: String, options: List<String>, value: String?, enabled: Boolean = true, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) {
            Text("$label: ${value?.ifBlank { if (label == "Categoría") "Sin categoría" else "Sin subcategoría" } ?: "Todas"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todas") }, onClick = { expanded = false; onSelect(null) })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.ifBlank { if (label == "Categoría") "Sin categoría" else "Sin subcategoría" }) }, onClick = { expanded = false; onSelect(option) })
            }
        }
    }
}

@Composable
fun ProductBrowser(products: List<Product>, categoryButtons: Boolean = false, content: @Composable (List<Product>) -> Unit) {
    var category by remember { mutableStateOf<String?>(null) }
    var subcategory by remember { mutableStateOf<String?>(null) }
    val categories = products.map { it.category }.distinct().sorted()
    val activeCategory = category?.takeIf { it in categories }
    val subcategories = products.filter { it.category == activeCategory }.map { it.subcategory }.distinct().sorted()
    val activeSubcategory = subcategory?.takeIf { activeCategory != null && it in subcategories }
    LaunchedEffect(activeCategory, activeSubcategory) {
        category = activeCategory
        subcategory = activeSubcategory
    }
    if (products.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (categoryButtons) {
                Text("Categorías", style = MaterialTheme.typography.titleSmall)
                CategoryButtons(categories, activeCategory, "Sin categoría") { category = it; subcategory = null }
                if (activeCategory != null && subcategories.any { it.isNotBlank() }) {
                    Text("Subcategorías", style = MaterialTheme.typography.titleSmall)
                    CategoryButtons(subcategories, activeSubcategory, "Sin subcategoría") { subcategory = it }
                }
            } else {
                CategoryMenu("Categoría", categories, activeCategory) { category = it; subcategory = null }
                CategoryMenu("Subcategoría", subcategories, activeSubcategory, activeCategory != null) { subcategory = it }
                if (activeCategory == null) Text("Elige una categoría para filtrar por subcategoría.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    content(products.inCategory(activeCategory, activeSubcategory))
}

@Composable
private fun CategoryButtons(options: List<String>, selected: String?, emptyLabel: String, onSelect: (String?) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Todas") })
        options.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option.ifBlank { emptyLabel }) })
        }
    }
}

@Composable
fun CategoryInput(label: String, value: String, options: List<String>, enabled: Boolean = true, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(value = value, onValueChange = { onChange(it.take(60)) }, label = { Text(label) }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
        if (options.isNotEmpty()) Box {
            TextButton(onClick = { expanded = true }, enabled = enabled) { Text("Elegir existente") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onChange(option) }) }
            }
        }
    }
}
