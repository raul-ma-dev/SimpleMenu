package com.rama.simplemenu

data class SaleLineDraft(val name: String = "", val price: String = "", val quantity: String = "1", val category: String? = null, val subcategory: String? = null) {
    fun toSaleLine(): SaleLine? {
        val cents = parsePrice(price) ?: return null
        val units = quantity.toIntOrNull()?.takeIf { it in 1..999 } ?: return null
        return name.trim().takeIf { it.isNotEmpty() }?.let { SaleLine(it, cents, units, category, subcategory) }
    }
}

fun SaleLine.toDraft() = SaleLineDraft(name, decimalAmount(cents), quantity.toString(), category, subcategory)

internal fun salesWithReportCategories(sales: List<Sale>, products: List<Product>): List<Sale> = sales.map { sale ->
    sale.copy(lines = sale.lines.map { line ->
        val product = if (line.category == null) products.singleOrNull { it.name.equals(line.name, ignoreCase = true) } else null
        line.copy(category = line.category ?: product?.category ?: "", subcategory = line.subcategory ?: product?.subcategory ?: "")
    })
}
fun decimalAmount(cents: Long): String = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

fun ShopData.updateSale(sale: Sale): ShopData {
    val original = sales.first { it.id == sale.id }
    require(sale.day == original.day)
    require(sale.lines.isNotEmpty() && sale.lines.all { it.name.isNotBlank() && it.cents in 1..999999999L && it.quantity in 1..999 })
    return copy(sales = sales.map { if (it.id == sale.id) sale else it })
}

fun ShopData.deleteSale(id: Long): ShopData = copy(sales = sales.filter { it.id != id })

private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
private fun spreadsheetText(value: String): String =
    if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@') || value.startsWith('\t') || value.startsWith('\r')) "'$value" else value

fun dailySalesCsv(day: String, sales: List<Sale>): String {
    val daily = sales.filter { it.day == day }.sortedBy { it.id }
    val rows = mutableListOf(listOf("Tipo", "Fecha", "Pedido", "Producto", "Cantidad", "Precio unitario", "Importe"))
    daily.forEach { sale ->
        sale.lines.forEach { line ->
            rows.add(listOf("Artículo", day, sale.id.toString(), spreadsheetText(line.name), line.quantity.toString(), decimalAmount(line.cents), decimalAmount(line.total)))
        }
        rows.add(listOf("Total pedido", day, sale.id.toString(), "", sale.lines.sumOf { it.quantity }.toString(), "", decimalAmount(sale.total)))
    }
    rows.add(listOf("Total del día", day, "", "", daily.sumOf { s -> s.lines.sumOf { it.quantity } }.toString(), "", decimalAmount(daily.sumOf { it.total })))
    rows.add(listOf("Pedidos cerrados", day, daily.size.toString(), "", "", "", ""))
    // BOM keeps accented product names readable when opening the CSV in Excel.
    return "\uFEFF" + rows.joinToString("\r\n") { row -> row.joinToString(",", transform = ::csvCell) } + "\r\n"
}
