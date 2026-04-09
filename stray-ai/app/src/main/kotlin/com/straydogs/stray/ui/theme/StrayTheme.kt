package com.straydogs.stray.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StrayColorScheme = darkColorScheme(
    primary = StrayRed,
    onPrimary = StrayWhite,
    primaryContainer = StrayRedDark,
    onPrimaryContainer = StrayWhite,
    secondary = StrayGray,
    onSecondary = StrayWhite,
    secondaryContainer = StrayDarkContainer,
    onSecondaryContainer = StrayWhite,
    tertiary = StrayGrayLight,
    onTertiary = StrayBlack,
    background = StrayBlack,
    onBackground = StrayWhite,
    surface = StrayDarkSurface,
    onSurface = StrayWhite,
    surfaceVariant = StrayDarkContainer,
    onSurfaceVariant = StrayWhiteDim,
    outline = StrayDarkOutline,
    outlineVariant = StrayGray,
    error = StrayRedLight,
    onError = StrayBlack,
    errorContainer = StrayRedDark,
    onErrorContainer = StrayWhite,
    inverseSurface = StrayWhite,
    inverseOnSurface = StrayBlack,
    inversePrimary = StrayRedDark
)

@Composable
fun StrayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StrayColorScheme,
        typography = StrayTypography,
        content = content
    )
}
