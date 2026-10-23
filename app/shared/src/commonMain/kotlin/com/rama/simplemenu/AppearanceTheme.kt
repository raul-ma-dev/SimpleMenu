package com.rama.simplemenu

import androidx.compose.material3.*
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min

fun appearanceColor(value: Long): Color = Color(0xFF000000L or value)
val LocalAppAppearance = staticCompositionLocalOf { AppAppearance() }
fun Color.rgbValue(): Long = toArgb().toLong() and 0xFFFFFFL
fun cardBackgroundColor(appearance: AppAppearance): Color = appearance.cardBackground?.let(::appearanceColor)
    ?: appearanceColorScheme(appearance).surfaceContainerHighest
fun navigationBackgroundColor(appearance: AppAppearance): Color = appearance.navigationBackground?.let(::appearanceColor)
    ?: appearanceColorScheme(appearance).surfaceContainer
fun preferredTextColor(appearance: AppAppearance): Color = appearance.textColor?.let(::appearanceColor)
    ?: foregroundColor(appearanceColor(appearance.background))
private fun contrast(a: Color, b: Color): Float = (max(a.luminance(), b.luminance()) + .05f) / (min(a.luminance(), b.luminance()) + .05f)
fun foregroundColor(background: Color): Color = if (contrast(Color.Black, background) >= contrast(Color.White, background)) Color.Black else Color.White

// Keep labels readable even when the chosen accent and background are very similar.
fun legibleAccent(accent: Color, background: Color): Color {
    if (contrast(accent, background) >= 4.5f) return accent
    val target = foregroundColor(background)
    return (1..20).map { lerp(accent, target, it / 20f) }.firstOrNull { contrast(it, background) >= 4.5f } ?: target
}

fun appearanceColorScheme(appearance: AppAppearance): ColorScheme {
    val background = appearanceColor(appearance.background)
    val foreground = legibleAccent(preferredTextColor(appearance), background)
    val primary = legibleAccent(appearanceColor(appearance.primary), background)
    val secondary = legibleAccent(appearanceColor(appearance.secondary), background)
    val primaryContainer = lerp(primary, background, .86f)
    val secondaryContainer = lerp(secondary, background, .86f)
    val base = if (background.luminance() < .4f) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = foregroundColor(primary),
        primaryContainer = primaryContainer, onPrimaryContainer = foregroundColor(primaryContainer),
        secondary = secondary, onSecondary = foregroundColor(secondary),
        secondaryContainer = secondaryContainer, onSecondaryContainer = foregroundColor(secondaryContainer),
        tertiary = primary, onTertiary = foregroundColor(primary),
        tertiaryContainer = primaryContainer, onTertiaryContainer = foregroundColor(primaryContainer),
        background = background, onBackground = foreground,
        surface = background, onSurface = foreground,
        surfaceVariant = lerp(background, foreground, .1f), onSurfaceVariant = lerp(foreground, background, .2f),
        surfaceTint = primary,
        surfaceBright = lerp(background, Color.White, .06f), surfaceDim = lerp(background, Color.Black, .06f),
        surfaceContainerLowest = background,
        surfaceContainerLow = lerp(background, foreground, .025f),
        surfaceContainer = lerp(background, foreground, .05f),
        surfaceContainerHigh = lerp(background, foreground, .075f),
        surfaceContainerHighest = lerp(background, foreground, .1f),
        outline = lerp(background, foreground, .55f), outlineVariant = lerp(background, foreground, .25f),
    )
}
