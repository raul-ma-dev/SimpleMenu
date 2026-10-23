package com.rama.simplemenu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
actual fun rememberShopStorage(): ShopStorage {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        object : ShopStorage {
            private val prefs = context.getSharedPreferences("simple_menu", 0)
            override fun read() = prefs.getString("shop", "") ?: ""
            override fun write(value: String) { check(prefs.edit().putString("shop", value).commit()) { "No se pudo guardar" } }
            override fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            override fun timestamp() = System.currentTimeMillis()
        }
    }
}
