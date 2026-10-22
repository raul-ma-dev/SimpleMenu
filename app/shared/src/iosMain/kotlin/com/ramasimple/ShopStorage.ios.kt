package com.ramasimple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970

@Composable
actual fun rememberShopStorage(): ShopStorage = remember {
    object : ShopStorage {
        override fun read() = NSUserDefaults.standardUserDefaults.stringForKey("simple_menu_shop") ?: ""
        override fun write(value: String) { NSUserDefaults.standardUserDefaults.setObject(value, forKey = "simple_menu_shop") }
        override fun today(): String = NSDateFormatter().apply { dateFormat = "yyyy-MM-dd" }.stringFromDate(NSDate())
        override fun timestamp() = (NSDate().timeIntervalSince1970 * 1000).toLong()
    }
}
