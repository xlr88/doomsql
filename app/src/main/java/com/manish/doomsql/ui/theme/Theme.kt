package com.manish.doomsql.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DoomPrimaryDark,
    onPrimary = DoomOnPrimaryDark,
    primaryContainer = DoomPrimaryContainerDark,
    onPrimaryContainer = DoomOnPrimaryContainerDark,
    secondary = DoomSecondaryDark,
    onSecondary = DoomOnSecondaryDark,
    secondaryContainer = DoomSecondaryContainerDark,
    onSecondaryContainer = DoomOnSecondaryContainerDark,
    background = DoomBackgroundDark,
    onBackground = DoomOnBackgroundDark,
    surface = DoomSurfaceDark,
    onSurface = DoomOnSurfaceDark,
    surfaceVariant = DoomSurfaceVariantDark,
    onSurfaceVariant = DoomOnSurfaceVariantDark,
    outline = DoomOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = DoomPrimaryLight,
    onPrimary = DoomOnPrimaryLight,
    primaryContainer = DoomPrimaryContainerLight,
    onPrimaryContainer = DoomOnPrimaryContainerLight,
    secondary = DoomSecondaryLight,
    onSecondary = DoomOnSecondaryLight,
    secondaryContainer = DoomSecondaryContainerLight,
    onSecondaryContainer = DoomOnSecondaryContainerLight,
    background = DoomBackgroundLight,
    onBackground = DoomOnBackgroundLight,
    surface = DoomSurfaceLight,
    onSurface = DoomOnSurfaceLight,
    surfaceVariant = DoomSurfaceVariantLight,
    onSurfaceVariant = DoomOnSurfaceVariantLight,
    outline = DoomOutlineLight
)

@Composable
fun DoomSqlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our cohesive curated palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
