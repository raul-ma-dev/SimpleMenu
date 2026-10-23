package com.rama.simplemenu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    outlined: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appearance = LocalAppAppearance.current
    val background = cardBackgroundColor(appearance)
    val foreground = legibleAccent(preferredTextColor(appearance), background)
    val theme = MaterialTheme.colorScheme
    val primary = legibleAccent(appearanceColor(appearance.primary), background)
    val secondary = legibleAccent(appearanceColor(appearance.secondary), background)
    val primaryContainer = lerp(primary, background, .86f)
    val secondaryContainer = lerp(secondary, background, .86f)
    MaterialTheme(colorScheme = theme.copy(
        surface = background, onSurface = foreground,
        primary = primary, onPrimary = foregroundColor(primary),
        primaryContainer = primaryContainer, onPrimaryContainer = legibleAccent(preferredTextColor(appearance), primaryContainer),
        secondary = secondary, onSecondary = foregroundColor(secondary),
        secondaryContainer = secondaryContainer, onSecondaryContainer = legibleAccent(preferredTextColor(appearance), secondaryContainer),
        onSurfaceVariant = foreground,
    )) {
        val colors = CardDefaults.cardColors(containerColor = background, contentColor = foreground)
        val actualBorder = border ?: if (outlined) BorderStroke(1.dp, theme.outlineVariant) else null
        if (onClick == null) Card(modifier = modifier, colors = colors, border = actualBorder, content = content)
        else Card(onClick = onClick, modifier = modifier, colors = colors, border = actualBorder, content = content)
    }
}

@Composable
fun AppNavigationBar(appearance: AppAppearance, selected: Int, preview: Boolean = false, onSelect: (Int) -> Unit) {
    val background = navigationBackgroundColor(appearance)
    val text = legibleAccent(preferredTextColor(appearance), background)
    val accent = legibleAccent(appearanceColor(appearance.primary), background)
    val indicator = lerp(accent, background, .8f)
    NavigationBar(
        containerColor = background,
        contentColor = text,
        tonalElevation = 0.dp,
        windowInsets = if (preview) WindowInsets(0, 0, 0, 0) else NavigationBarDefaults.windowInsets,
    ) {
        listOf("Productos", "Nuevo pedido", "Ventas", "Personalizar").forEachIndexed { index, title ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Text(listOf("▦", "+", "$ ", "⚙")[index]) },
                label = { Text(title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = legibleAccent(preferredTextColor(appearance), indicator),
                    selectedTextColor = text,
                    indicatorColor = indicator,
                    unselectedIconColor = text,
                    unselectedTextColor = text,
                ),
            )
        }
    }
}
