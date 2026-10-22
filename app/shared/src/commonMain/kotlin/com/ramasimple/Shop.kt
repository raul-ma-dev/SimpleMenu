package com.ramasimple

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
data class SaleLine(val name: String, val cents: Long, val quantity: Int) {
    val total get() = cents * quantity
}
data class Sale(val id: Long, val day: String, val lines: List<SaleLine>) {
    val total get() = lines.sumOf { it.total }
}
data class ShopData(val products: List<Product> = emptyList(), val sales: List<Sale> = emptyList())
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
    products.forEach { add("P|${it.id}|${it.name.pack()}|${it.cents}|${it.category.pack()}|${it.subcategory.pack()}|${(it.photo ?: "").pack()}") }
    sales.forEach { sale ->
        add("S|${sale.id}|${sale.day}")
        sale.lines.forEach { add("L|${it.name.pack()}|${it.cents}|${it.quantity}") }
    }
}.joinToString("\n")
fun decodeShop(text: String): ShopData {
    val products = mutableListOf<Product>()
    val sales = mutableListOf<Sale>()
    if (text.isBlank()) return ShopData()
    text.lines().filter { it.isNotBlank() }.forEach { row ->
        val p = row.split('|')
        when (p[0]) {
            "P" -> products.add(Product(
                p[1].toLong(), p[2].unpack(), p[3].toLong(),
                p.getOrNull(4)?.unpack() ?: "",
                p.getOrNull(5)?.unpack() ?: "",
                p.getOrNull(6)?.unpack()?.takeIf { it.isNotBlank() },
            ))
            "S" -> sales.add(Sale(p[1].toLong(), p[2], emptyList()))
            "L" -> { val last = sales.removeAt(sales.lastIndex); sales.add(last.copy(lines = last.lines + SaleLine(p[1].unpack(), p[2].toLong(), p[3].toInt()))) }
            else -> error("Formato de datos desconocido")
        }
    }
    return ShopData(products, sales)
}
