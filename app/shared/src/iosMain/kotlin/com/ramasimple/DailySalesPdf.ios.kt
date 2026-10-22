@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@file:Suppress("CAST_NEVER_SUCCEEDS")
package com.ramasimple

import kotlinx.cinterop.*
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSMutableData
import platform.Foundation.NSString
import platform.UIKit.*
import platform.posix.memcpy

actual fun dailySalesPdf(day: String, sales: List<Sale>): ByteArray {
    fun attributes(size: Float, bold: Boolean, color: ReportColor = reportInk): Map<Any?, Any> = mapOf(
        NSFontAttributeName to if (bold) UIFont.boldSystemFontOfSize(size.toDouble()) else UIFont.systemFontOfSize(size.toDouble()),
        NSForegroundColorAttributeName to UIColor(red = color.red.toDouble(), green = color.green.toDouble(), blue = color.blue.toDouble(), alpha = 1.0),
    )
    val pages = dailyPdfPages(day, sales) { text, size, bold ->
        (text as NSString).sizeWithAttributes(attributes(size, bold)).useContents { width.toFloat() }
    }
    val data = NSMutableData()
    val bounds = CGRectMake(0.0, 0.0, REPORT_WIDTH.toDouble(), REPORT_HEIGHT.toDouble())
    UIGraphicsBeginPDFContextToData(data, bounds, null)
    try {
        pages.forEach { commands ->
            UIGraphicsBeginPDFPageWithInfo(bounds, null)
            commands.forEach { command ->
                when (command) {
                    is ReportText -> (command.value as NSString).drawAtPoint(CGPointMake(command.x.toDouble(), command.y.toDouble()), withAttributes = attributes(command.size, command.bold, command.color))
                    is ReportBox -> {
                        UIColor(red = command.color.red.toDouble(), green = command.color.green.toDouble(), blue = command.color.blue.toDouble(), alpha = 1.0).setFill()
                        UIRectFill(CGRectMake(command.x.toDouble(), command.y.toDouble(), command.width.toDouble(), command.height.toDouble()))
                    }
                }
            }
        }
    } finally { UIGraphicsEndPDFContext() }
    return ByteArray(data.length.toInt()).also { bytes ->
        if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    }
}
