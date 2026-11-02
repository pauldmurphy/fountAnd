package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Noir Typewriter Studio — Dark Palette (Default for Focused Writing Sessions)
val ObsidianBg = Color(0xFF111315)
val WarmCharcoalSurface = Color(0xFF181B1F)
val ElevatedSlateCard = Color(0xFF21262D)
val AmberGoldPrimary = Color(0xFFE5A93C)
val AmberGoldOnPrimary = Color(0xFF231700)
val AmberGoldContainer = Color(0xFF3E2D0B)
val OnAmberGoldContainer = Color(0xFFFFDFAC)

val CinemaCyanSecondary = Color(0xFF68B0AB)
val OnCinemaCyanSecondary = Color(0xFF00201E)
val CinemaCyanContainer = Color(0xFF173B39)
val OnCinemaCyanContainer = Color(0xFFB5ECE7)

val RoseCueTertiary = Color(0xFFE08D79)
val WarmParchmentText = Color(0xFFECE8DF)
val MutedPewterText = Color(0xFF9DA3AE)
val SubtleDividerDark = Color(0xFF2B313A)

// Daylight Manuscript Mode — Light Palette
val CreamPaperBg = Color(0xFFFAF6EE)
val AgedIvorySurface = Color(0xFFF1ECE1)
val WarmSandContainer = Color(0xFFE6DFD1)
val TerracottaInkPrimary = Color(0xFF9E472A)
val OnTerracottaPrimary = Color(0xFFFFFFFF)
val TerracottaContainer = Color(0xFFFFDBD0)
val OnTerracottaContainer = Color(0xFF390C00)

val SlateTealSecondary = Color(0xFF2B5B6C)
val OnSlateTealSecondary = Color(0xFFFFFFFF)
val SlateTealContainer = Color(0xFFC7E7F5)
val OnSlateTealContainer = Color(0xFF001E28)

val PlumTertiary = Color(0xFF6B4D7B)
val CarbonInkText = Color(0xFF1C1B19)
val WarmSlateMutedText = Color(0xFF5D5952)
val SubtleDividerLight = Color(0xFFD8D0C2)

@Immutable
data class FountainSyntaxColors(
    val sceneHeading: Color,
    val sceneHeadingBg: Color,
    val characterCue: Color,
    val dialogue: Color,
    val parenthetical: Color,
    val transition: Color,
    val action: Color,
    val titleKey: Color,
    val titleValue: Color,
    val note: Color,
    val sectionAndSynopsis: Color,
    val centeredAndLyrics: Color,
    val syntaxMarker: Color,
    val paperSurface: Color,
    val paperBorder: Color,
    val lineNumber: Color
)

val DarkFountainSyntaxColors = FountainSyntaxColors(
    sceneHeading = Color(0xFF68B0AB),
    sceneHeadingBg = Color(0xFF19282A),
    characterCue = Color(0xFFE5A93C),
    dialogue = Color(0xFFF3EFE6),
    parenthetical = Color(0xFFA7B4C2),
    transition = Color(0xFFE08D79),
    action = Color(0xFFDCD7CC),
    titleKey = Color(0xFF8AB4F8),
    titleValue = Color(0xFFE5A93C),
    note = Color(0xFF9CCC65),
    sectionAndSynopsis = Color(0xFFBA8FF7),
    centeredAndLyrics = Color(0xFF4DD0E1),
    syntaxMarker = Color(0xFF6E7681),
    paperSurface = Color(0xFF16191D),
    paperBorder = Color(0xFF282E37),
    lineNumber = Color(0xFF555D69)
)

val LightFountainSyntaxColors = FountainSyntaxColors(
    sceneHeading = Color(0xFF1B5E6F),
    sceneHeadingBg = Color(0xFFE6EFEF),
    characterCue = Color(0xFF9E472A),
    dialogue = Color(0xFF1C1B19),
    parenthetical = Color(0xFF5A5248),
    transition = Color(0xFF8C2D46),
    action = Color(0xFF262421),
    titleKey = Color(0xFF2B5B6C),
    titleValue = Color(0xFF7D3C1D),
    note = Color(0xFF3E6B27),
    sectionAndSynopsis = Color(0xFF6B4D7B),
    centeredAndLyrics = Color(0xFF1F6E7A),
    syntaxMarker = Color(0xFF968E82),
    paperSurface = Color(0xFFFFFDF9),
    paperBorder = Color(0xFFDDD5C7),
    lineNumber = Color(0xFF9B9386)
)

val LocalFountainSyntaxColors = staticCompositionLocalOf { DarkFountainSyntaxColors }
