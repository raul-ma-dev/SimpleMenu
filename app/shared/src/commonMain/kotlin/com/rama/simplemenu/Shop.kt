package com.rama.simplemenu

import androidx.compose.runtime.Composable

data class Product(
    val id: Long,
    val name: String,
    val cents: Long,
    val category: String = "",
    val subcategory: String = "",
    val photo: String? = null,
)

fun List<Product>.inCategory(category: String?, subcategory: String?): List<Product> = filter {
    (category == null || it.category == category) && (subcategory == null || it.subcategory == subcategory)
}

fun Product.categoryLabel(): String = listOf(category.ifBlank { "Sin categoría" }, subcategory)
    .filter { it.isNotBlank() }.joinToString(" / ")
data class SaleLine(val name: String, val cents: Long, val quantity: Int, val category: String? = null, val subcategory: String? = null) {
    val total get() = cents * quantity
}
data class Sale(val id: Long, val day: String, val lines: List<SaleLine>) {
    val total get() = lines.sumOf { it.total }
}
data class AppAppearance(
    val businessName: String = "Simple Menu",
    val logo: String? = null,
    val primary: Long = 0x146C59,
    val secondary: Long = 0x55645D,
    val background: Long = 0xF7F9F6,
    val cardBackground: Long? = null,
    val textColor: Long? = null,
    val navigationBackground: Long? = null,
)

fun parseHexColor(text: String): Long? = text.trim().removePrefix("#")
    .takeIf { Regex("[a-fA-F0-9]{6}").matches(it) }?.toLongOrNull(16)
fun hexColor(value: Long): String = "#${value.toString(16).uppercase().padStart(6, '0')}"

data class ShopData(
    val products: List<Product> = emptyList(),
    val sales: List<Sale> = emptyList(),
    val appearance: AppAppearance = AppAppearance(),
)
interface ShopStorage {
    fun read(): String
    fun write(value: String)
    fun today(): String
    fun timestamp(): Long
}
@Composable expect fun rememberShopStorage(): ShopStorage

fun money(cents: Long): String = "$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
fun parsePrice(text: String): Long? {
    val value = text.trim().replace(',', '.')
    if (!Regex("[0-9]{1,7}(\\.[0-9]{1,2})?").matches(value)) return null
    val parts = value.split('.')
    return (parts[0].toLong() * 100 + (parts.getOrNull(1) ?: "").padEnd(2, '0').toLong()).takeIf { it > 0 }
}
private fun String.pack() = map { it.code.toString(16).padStart(4, '0') }.joinToString("")
private fun String.unpack() = chunked(4).map { it.toInt(16).toChar() }.joinToString("")
fun ShopData.encode(): String = buildList {
    add("C|${appearance.businessName.pack()}|${(appearance.logo ?: "").pack()}|${hexColor(appearance.primary)}|${hexColor(appearance.secondary)}|${hexColor(appearance.background)}|${appearance.cardBackground?.let(::hexColor) ?: ""}|${appearance.textColor?.let(::hexColor) ?: ""}|${appearance.navigationBackground?.let(::hexColor) ?: ""}")
    products.forEach { add("P|${it.id}|${it.name.pack()}|${it.cents}|${it.category.pack()}|${it.subcategory.pack()}|${(it.photo ?: "").pack()}") }
    sales.forEach { sale ->
        add("S|${sale.id}|${sale.day}")
        sale.lines.forEach { add("L|${it.name.pack()}|${it.cents}|${it.quantity}|${it.category?.pack() ?: "~"}|${it.subcategory?.pack() ?: "~"}") }
    }
}.joinToString("\n")
fun decodeShop(text: String): ShopData {
    val products = mutableListOf<Product>()
    val sales = mutableListOf<Sale>()
    var appearance = AppAppearance()
    if (text.isBlank()) return ShopData()
    text.lines().filter { it.isNotBlank() }.forEach { row ->
        val p = row.split('|')
        when (p[0]) {
            "C" -> appearance = AppAppearance(
                businessName = p[1].unpack().ifBlank { "Simple Menu" },
                logo = p[2].unpack().takeIf { it.isNotBlank() },
                primary = requireNotNull(parseHexColor(p[3])),
                secondary = requireNotNull(parseHexColor(p[4])),
                background = requireNotNull(parseHexColor(p[5])),
                cardBackground = p.getOrNull(6)?.takeIf { it.isNotBlank() }?.let { requireNotNull(parseHexColor(it)) },
                textColor = p.getOrNull(7)?.takeIf { it.isNotBlank() }?.let { requireNotNull(parseHexColor(it)) },
                navigationBackground = p.getOrNull(8)?.takeIf { it.isNotBlank() }?.let { requireNotNull(parseHexColor(it)) },
            )
            "P" -> products.add(Product(
                p[1].toLong(), p[2].unpack(), p[3].toLong(),
                p.getOrNull(4)?.unpack() ?: "",
                p.getOrNull(5)?.unpack() ?: "",
                p.getOrNull(6)?.unpack()?.takeIf { it.isNotBlank() },
            ))
            "S" -> sales.add(Sale(p[1].toLong(), p[2], emptyList()))
            "L" -> { val last = sales.removeAt(sales.lastIndex); sales.add(last.copy(lines = last.lines + SaleLine(
                p[1].unpack(), p[2].toLong(), p[3].toInt(),
                p.getOrNull(4)?.takeUnless { it == "~" }?.unpack(),
                p.getOrNull(5)?.takeUnless { it == "~" }?.unpack(),
            ))) }
            else -> error("Formato de datos desconocido")
        }
    }
    return ShopData(products, sales, appearance)
}
