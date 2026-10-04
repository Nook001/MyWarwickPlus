package uk.ac.warwick.plus.ui

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ColourTheme(val id: String, val label: String, val description: String) {
    FOREST("forest", "Forest", "Forest green"), LAKE("lake", "Lake", "Mist blue"),
    HEATHER("heather", "Heather", "Soft purple"), SAND("sand", "Sand", "Warm sandstone"),
    ROSEWOOD("rosewood", "Rosewood", "Deep burgundy");
    companion object { fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: FOREST }
}

data class Appearance(val theme: ColourTheme = ColourTheme.FOREST, val texture: Boolean = false)
private fun hex(value: Long) = Color(value or 0xFF000000)

data class FixedPalette(val background: Color, val text: Color, val muted: Color, val accent: Color,
    val next: Color, val nextText: Color, val card: Color, val empty: Color, val spots: List<Color>,
    val darkSystemIcons: Boolean, val error: Color, val errorContainer: Color, val onError: Color,
    val onErrorContainer: Color) {
    // All roles come from one fixed palette; system appearance never selects another palette.
    val scheme = lightColorScheme(
        primary = accent, onPrimary = background, primaryContainer = next, onPrimaryContainer = nextText,
        inversePrimary = background, secondary = accent, onSecondary = background,
        secondaryContainer = next, onSecondaryContainer = nextText, tertiary = accent,
        onTertiary = background, tertiaryContainer = next, onTertiaryContainer = nextText,
        background = background, onBackground = text, surface = background, onSurface = text,
        surfaceVariant = empty, onSurfaceVariant = muted, surfaceTint = accent,
        inverseSurface = text, inverseOnSurface = background, error = error, onError = onError,
        errorContainer = errorContainer, onErrorContainer = onErrorContainer,
        outline = muted, outlineVariant = lerp(card, muted, .35f), scrim = Color.Black,
        surfaceBright = card, surfaceDim = empty, surfaceContainerLowest = card,
        surfaceContainerLow = card, surfaceContainer = empty, surfaceContainerHigh = lerp(card, next, .4f),
        surfaceContainerHighest = next,
        primaryFixed = next, primaryFixedDim = next, onPrimaryFixed = nextText, onPrimaryFixedVariant = nextText,
        secondaryFixed = next, secondaryFixedDim = next, onSecondaryFixed = nextText, onSecondaryFixedVariant = nextText,
        tertiaryFixed = next, tertiaryFixedDim = next, onTertiaryFixed = nextText, onTertiaryFixedVariant = nextText)
}

private fun palette(background: Long, text: Long, muted: Long, accent: Long, next: Long,
    nextText: Long, card: Long, empty: Long, spots: List<Long>, darkIcons: Boolean,
    error: Long, errorContainer: Long, onError: Long, onErrorContainer: Long) = FixedPalette(
        hex(background), hex(text), hex(muted), hex(accent), hex(next), hex(nextText), hex(card), hex(empty),
        spots.map(::hex), darkIcons, hex(error), hex(errorContainer), hex(onError), hex(onErrorContainer))

private val palettes = mapOf(
    ColourTheme.FOREST to palette(0x111B17, 0xE5ECE7, 0xBDCDC3, 0xBFE2CA, 0x254636, 0xDCECDF,
        0x1C2822, 0x202D25, listOf(0x3D6854, 0x254653, 0x545B3E), false, 0xFFB4AB, 0x5C211D, 0x39100B, 0xFFDED9),
    ColourTheme.LAKE to palette(0xF0F6FA, 0x1D303C, 0x344751, 0x1E475C, 0xD6E8F1, 0x183743,
        0xFBFDFE, 0xE5EEF3, listOf(0x79B9DA, 0xA6DCD9, 0xAABBE3), true, 0x7F1D16, 0xFFE7E3, 0xFFFFFF, 0x61100C),
    ColourTheme.HEATHER to palette(0xF5F1FA, 0x2F2A3A, 0x473A55, 0x523367, 0xE4DDF2, 0x3D3155,
        0xFDFCFE, 0xEBE6F1, listOf(0xBD9BD9, 0xD5A9C5, 0xB0C2DD), true, 0x7F1D16, 0xFFE7E3, 0xFFFFFF, 0x61100C),
    ColourTheme.SAND to palette(0xF8F3EA, 0x342B23, 0x524133, 0x5D3D23, 0xEFE0C8, 0x4C3824,
        0xFFFCF6, 0xF0E9DF, listOf(0xDDB579, 0xDFAF99, 0xBDC5A4), true, 0x7F1D16, 0xFFE7E3, 0xFFFFFF, 0x61100C),
    ColourTheme.ROSEWOOD to palette(0x21181D, 0xF0E5E9, 0xD7C3CD, 0xF0C2D1, 0x4B2C38, 0xF5DEE6,
        0x31232B, 0x2D2229, listOf(0x754153, 0x514159, 0x625039), false, 0xFFB4AB, 0x5C211D, 0x39100B, 0xFFDED9))

fun ColourTheme.palette(): FixedPalette = palettes.getValue(this)
val LocalAppearance = staticCompositionLocalOf { Appearance() }
val LocalAppearanceChange = staticCompositionLocalOf<(Appearance) -> Unit> { {} }

@Composable
fun PlusTheme(appearance: Appearance = Appearance(), onAppearanceChange: (Appearance) -> Unit = {}, content: @Composable () -> Unit) {
    val palette = appearance.theme.palette()
    val view = LocalView.current
    LaunchedEffect(view, appearance.theme) {
        (view.context as? Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = palette.darkSystemIcons
                isAppearanceLightNavigationBars = palette.darkSystemIcons
            }
        }
    }
    CompositionLocalProvider(LocalAppearance provides appearance, LocalAppearanceChange provides onAppearanceChange) {
        MaterialTheme(colorScheme = palette.scheme, content = content)
    }
}
