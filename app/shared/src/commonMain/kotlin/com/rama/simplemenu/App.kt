package com.rama.simplemenu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    val logoEditor = remember(scope) { ProductPhotoEditor(scope, logo = true) }
    val pickLogo = rememberProductPhotoPicker(logoEditor, tab == 3)
    val dailyReport = rememberDailyReportActions()
    fun openEditor(product: Product?) {
        editing = product
        photoEditor.open(product?.photo)
        showEditor = true
    }
    fun dismissEditor() {
        if (!photoEditor.busy) { photoEditor.discard(); showEditor = false }
    }
    fun save(next: ShopData): Boolean = try {
        storage.write(next.encode()); data = next; error = null; true
    } catch (_: Exception) { error = "No se pudieron guardar los cambios. Intenta de nuevo."; false }
    val shop = data ?: ShopData()
    val lines = shop.products.mapNotNull { p -> cart[p.id]?.takeIf { it > 0 }?.let { SaleLine(p.name, p.cents, it) } }
    CompositionLocalProvider(LocalAppAppearance provides shop.appearance) {
    MaterialTheme(colorScheme = appearanceColorScheme(shop.appearance)) {
        Scaffold(bottomBar = {
            AppNavigationBar(shop.appearance, selected = tab) { index ->
                        if (index != tab) {
                            if (tab == 3) logoEditor.discard()
                            if (index == 3) logoEditor.open(shop.appearance.logo)
                            tab = index
                        }
                        if (index == 2) selectedDay = storage.today()
            }
        }) { padding ->
            if (tab == 1) {
                NewOrderScreen(
                    appearance = shop.appearance,
                    products = shop.products,
                    cart = cart,
                    enabled = data != null,
                    error = error,
                    notice = notice,
                    onQuantityChange = { product, quantity ->
                        cart = if (quantity <= 0) cart - product.id else cart + (product.id to quantity.coerceAtMost(999))
                    },
                    onCloseOrder = { closing = true },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(8.dp))
                BusinessHeader(shop.appearance)
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
                    2 -> {
                        SalesSection(
                            shop = shop,
                            today = storage.today(),
                            selectedDay = selectedDay,
                            onDayChange = { selectedDay = it },
                            onSave = { save(it) },
                            report = dailyReport,
                        )
                    }
                    3 -> {
                        AppearanceSection(
                            saved = shop.appearance,
                            logoEditor = logoEditor,
                            pickLogo = pickLogo,
                            enabled = data != null,
                            onSave = { appearance ->
                                val saved = save(shop.copy(appearance = appearance))
                                if (saved) logoEditor.commit()
                                saved
                            },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
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
}
