package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NoirDarkColorScheme = darkColorScheme(
    primary = AmberGoldPrimary,
    onPrimary = AmberGoldOnPrimary,
    primaryContainer = AmberGoldContainer,
    onPrimaryContainer = OnAmberGoldContainer,
    secondary = CinemaCyanSecondary,
    onSecondary = OnCinemaCyanSecondary,
    secondaryContainer = CinemaCyanContainer,
    onSecondaryContainer = OnCinemaCyanContainer,
    tertiary = RoseCueTertiary,
    background = ObsidianBg,
    onBackground = WarmParchmentText,
    surface = WarmCharcoalSurface,
    onSurface = WarmParchmentText,
    surfaceVariant = ElevatedSlateCard,
    onSurfaceVariant = MutedPewterText,
    outline = SubtleDividerDark,
    outlineVariant = SubtleDividerDark
)

private val ManuscriptLightColorScheme = lightColorScheme(
    primary = TerracottaInkPrimary,
    onPrimary = OnTerracottaPrimary,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = SlateTealSecondary,
    onSecondary = OnSlateTealSecondary,
    secondaryContainer = SlateTealContainer,
    onSecondaryContainer = OnSlateTealContainer,
    tertiary = PlumTertiary,
    background = CreamPaperBg,
    onBackground = CarbonInkText,
    surface = AgedIvorySurface,
    onSurface = CarbonInkText,
    surfaceVariant = WarmSandContainer,
    onSurfaceVariant = WarmSlateMutedText,
    outline = SubtleDividerLight,
    outlineVariant = SubtleDividerLight
)

@Composable
fun FountainStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NoirDarkColorScheme else ManuscriptLightColorScheme
    val syntaxColors = if (darkTheme) DarkFountainSyntaxColors else LightFountainSyntaxColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalFountainSyntaxColors provides syntaxColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FountainStudioTheme(darkTheme = darkTheme, content = content)
}
