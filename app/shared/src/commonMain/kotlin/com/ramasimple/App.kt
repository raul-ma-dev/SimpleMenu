package com.ramasimple

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun App() {
    val storage = rememberShopStorage()
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf(runCatching { decodeShop(storage.read()) }.getOrElse {
        error = "No se pudieron leer los datos. Reinicia la app antes de realizar cambios."
        null
    }) }
    var tab by remember { mutableStateOf(0) }
    var cart by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Product?>(null) }
    var selectedDay by remember { mutableStateOf(storage.today()) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val photoEditor = remember(scope) { ProductPhotoEditor(scope) }
    val pickPhoto = rememberProductPhotoPicker(photoEditor, showEditor)
    fun openEditor(product: Product?) {
        editing = product
        photoEditor.open(product?.photo)
        showEditor = true
    }
    fun dismissEditor() {
        if (!photoEditor.busy) { photoEditor.discard(); showEditor = false }
    }
    fun save(next: ShopData): Boolean = try {
        storage.write(next.encode()); data = next; true
    } catch (_: Exception) { error = "No se pudo guardar. Intenta de nuevo; el pedido sigue abierto."; false }
    val shop = data ?: ShopData()
    val lines = shop.products.mapNotNull { p -> cart[p.id]?.takeIf { it > 0 }?.let { SaleLine(p.name, p.cents, it) } }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF146C59), secondary = Color(0xFF55645D), background = Color(0xFFF7F9F6))) {
        Scaffold(bottomBar = {
            NavigationBar {
                listOf("Productos", "Nuevo pedido", "Ventas").forEachIndexed { index, title ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index; if (index == 2) selectedDay = storage.today() },
                        icon = { Text(listOf("▦", "+", "$ ")[index]) }, label = { Text(title) })
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(8.dp))
                Text("Simple Menu", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("El control de tu negocio, al día", color = MaterialTheme.colorScheme.secondary)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                when (tab) {
                    0 -> {
                        Text("Productos", style = MaterialTheme.typography.headlineSmall)
                        Button(onClick = { openEditor(null) }, enabled = data != null) { Text("Agregar producto") }
                        if (shop.products.isEmpty()) Text("Agrega tu primer producto con su nombre y precio para empezar a vender.")
                        ProductBrowser(shop.products) { filtered ->
                            ProductGrid(
                                products = filtered,
                                cart = cart,
                                onEdit = { openEditor(it) },
                                onDelete = { deleting = it },
                            )
                        }
                    }
                    1 -> {
                        Text("Nuevo pedido", style = MaterialTheme.typography.headlineSmall)
                        if (shop.products.isEmpty()) Text("Primero agrega productos en la sección Productos.")
                        ProductBrowser(shop.products) { filtered ->
                        filtered.forEach { p ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    ProductPhoto(p.photo, p.name, Modifier.fillMaxWidth().height(140.dp))
                                    Text(p.name, fontWeight = FontWeight.Bold)
                                    Text(p.categoryLabel(), style = MaterialTheme.typography.bodySmall)
                                    Text(money(p.cents))
                                    val quantity = cart[p.id] ?: 0
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        TextButton(enabled = quantity > 0, onClick = { cart = if (quantity == 1) cart - p.id else cart + (p.id to quantity - 1) }) { Text("−") }
                                        Text("$quantity", modifier = Modifier.padding(top = 12.dp))
                                        TextButton(enabled = quantity < 999, onClick = { cart = cart + (p.id to quantity + 1) }) { Text("+") }
                                    }
                                    if (quantity > 0) Text("Subtotal: ${money(p.cents * quantity)}")
                                }
                            }
                        }
                        }
                        if (lines.isNotEmpty()) {
                            Text("Pedido actual", style = MaterialTheme.typography.titleMedium)
                            lines.forEach { Text("${it.quantity} × ${it.name} · ${money(it.total)}") }
                        }
                        Text("${lines.sumOf { it.quantity }} artículos · Total ${money(lines.sumOf { it.total })}", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = { closing = true }, enabled = lines.isNotEmpty() && data != null, modifier = Modifier.fillMaxWidth()) { Text("Cerrar pedido") }
                    }
                    2 -> {
                        Text("Ventas por día", style = MaterialTheme.typography.headlineSmall)
                        val days = (shop.sales.map { it.day } + storage.today()).distinct().sortedDescending()
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }) { Text("Fecha: $selectedDay ▾") }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                days.forEach { day -> DropdownMenuItem(text = { Text(day) }, onClick = { selectedDay = day; expanded = false }) }
                            }
                        }
                        val sales = shop.sales.filter { it.day == selectedDay }
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) {
                            Text("Total del día")
                            Text(money(sales.sumOf { it.total }), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Text("${sales.size} pedidos · ${sales.sumOf { s -> s.lines.sumOf { it.quantity } }} artículos")
                        } }
                        if (sales.isEmpty()) Text("Todavía no hay ventas registradas en esta fecha.")
                        sales.asReversed().forEach { sale ->
                            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Pedido #${sale.id}", fontWeight = FontWeight.Bold)
                                sale.lines.forEach { Text("${it.quantity} × ${it.name} · ${money(it.cents)} = ${money(it.total)}") }
                                HorizontalDivider()
                                Text("Total ${money(sale.total)}", fontWeight = FontWeight.Bold)
                            } }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        if (showEditor) {
            var name by remember(editing) { mutableStateOf(editing?.name ?: "") }
            var price by remember(editing) { mutableStateOf(editing?.let { "${it.cents / 100}.${(it.cents % 100).toString().padStart(2, '0')}" } ?: "") }
            var category by remember(editing) { mutableStateOf(editing?.category ?: "") }
            var subcategory by remember(editing) { mutableStateOf(editing?.subcategory ?: "") }
            val categories = shop.products.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
            val canonicalCategory = categories.firstOrNull { it.equals(category.trim(), true) } ?: category.trim()
            val subcategories = shop.products.filter { it.category == canonicalCategory }.map { it.subcategory }.filter { it.isNotBlank() }.distinct().sorted()
            val cents = parsePrice(price)
            val duplicate = shop.products.any { it.id != editing?.id && it.name.equals(name.trim(), ignoreCase = true) }
            AlertDialog(onDismissRequest = { dismissEditor() }, title = { Text(if (editing == null) "Agregar producto" else "Editar producto") }, text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Nombre") }, singleLine = true)
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio ($)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    CategoryInput("Categoría", category, categories) { next ->
                        if (!next.trim().equals(category.trim(), true)) subcategory = ""
                        category = next
                    }
                    CategoryInput("Subcategoría (opcional)", subcategory, subcategories, category.isNotBlank()) { subcategory = it }
                    Text("Escribe una nueva categoría o elige una existente.", style = MaterialTheme.typography.bodySmall)
                    ProductPhoto(photoEditor.photo, name.ifBlank { "producto" }, Modifier.fillMaxWidth().height(140.dp))
                    OutlinedButton(onClick = pickPhoto, enabled = !photoEditor.busy) { Text(if (photoEditor.photo == null) "Agregar foto" else "Cambiar foto") }
                    if (photoEditor.photo != null) TextButton(onClick = { photoEditor.remove() }, enabled = !photoEditor.busy) { Text("Quitar foto") }
                    if (photoEditor.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Guardando foto…") }
                    photoEditor.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Text(if (duplicate) "Ya existe un producto con ese nombre." else "Precio mayor a cero, con hasta dos decimales.")
                }
            }, confirmButton = { TextButton(enabled = name.isNotBlank() && category.isNotBlank() && cents != null && !duplicate && !photoEditor.busy, onClick = {
                val canonicalSubcategory = subcategories.firstOrNull { it.equals(subcategory.trim(), true) } ?: subcategory.trim()
                val product = Product(editing?.id ?: ((shop.products.maxOfOrNull { it.id } ?: 0) + 1), name.trim(), cents!!, canonicalCategory, canonicalSubcategory, photoEditor.photo)
                if (save(shop.copy(products = if (editing == null) shop.products + product else shop.products.map { if (it.id == product.id) product else it }))) {
                    photoEditor.commit()
                    showEditor = false
                }
            }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = { dismissEditor() }, enabled = !photoEditor.busy) { Text("Cancelar") } })
        }
        if (closing) AlertDialog(onDismissRequest = { closing = false }, title = { Text("Cerrar pedido") }, text = {
            Column { lines.forEach { Text("${it.quantity} × ${it.name}: ${money(it.total)}") }; Text("Total: ${money(lines.sumOf { it.total })}"); Text("Se registrará en la fecha ${storage.today()}.") }
        }, confirmButton = { TextButton(onClick = {
            val day = storage.today()
            val sale = Sale((shop.sales.maxOfOrNull { it.id } ?: 0) + 1, day, lines)
            if (save(shop.copy(sales = shop.sales + sale))) { cart = emptyMap(); closing = false; notice = "Pedido #${sale.id} guardado por ${money(sale.total)}" }
            else closing = false
        }) { Text("Confirmar venta") } }, dismissButton = { TextButton(onClick = { closing = false }) { Text("Seguir editando") } })
        deleting?.let { product -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Eliminar producto") }, text = { Text("¿Eliminar ${product.name}? Sus ventas anteriores se conservarán.") }, confirmButton = { TextButton(onClick = { if (save(shop.copy(products = shop.products.filter { it.id != product.id }))) { scope.launch { deleteProductPhoto(product.photo) }; deleting = null } }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }) }
    }
}
