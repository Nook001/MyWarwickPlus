package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.ui.components.*
import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ColourTheme(val id: String, val labelRes: Int, val descriptionRes: Int) {
    FOREST("forest", R.string.theme_forest, R.string.theme_forest_detail), LAKE("lake", R.string.theme_lake, R.string.theme_lake_detail),
    HEATHER("heather", R.string.theme_heather, R.string.theme_heather_detail), SAND("sand", R.string.theme_sand, R.string.theme_sand_detail),
    ROSEWOOD("rosewood", R.string.theme_rosewood, R.string.theme_rosewood_detail);
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
    ColourTheme.FOREST to palette(
        background = 0x111B17,
        text = 0xE5ECE7,
        muted = 0xCBD8CF,
        accent = 0xBFE2CA,
        next = 0x486950,
        nextText = 0xDCECDF,
        card = 0x465F4E,
        empty = 0x405747,
        spots = listOf(0x3D6854, 0x254653, 0x545B3E),
        darkIcons = false,
        error = 0xFFB4AB,
        errorContainer = 0x5C211D,
        onError = 0x39100B,
        onErrorContainer = 0xFFDED9),
    ColourTheme.LAKE to palette(
        background = 0xF0F6FA,
        text = 0x1D303C,
        muted = 0x344751,
        accent = 0x1E475C,
        next = 0xD6E8F1,
        nextText = 0x183743,
        card = 0xFBFDFE,
        empty = 0xE5EEF3,
        spots = listOf(0x79B9DA, 0xA6DCD9, 0xAABBE3),
        darkIcons = true,
        error = 0x7F1D16,
        errorContainer = 0xFFE7E3,
        onError = 0xFFFFFF,
        onErrorContainer = 0x61100C),
    ColourTheme.HEATHER to palette(
        background = 0xF5F1FA,
        text = 0x2F2A3A,
        muted = 0x473A55,
        accent = 0x523367,
        next = 0xE4DDF2,
        nextText = 0x3D3155,
        card = 0xFDFCFE,
        empty = 0xEBE6F1,
        spots = listOf(0xBD9BD9, 0xD5A9C5, 0xB0C2DD),
        darkIcons = true,
        error = 0x7F1D16,
        errorContainer = 0xFFE7E3,
        onError = 0xFFFFFF,
        onErrorContainer = 0x61100C),
    ColourTheme.SAND to palette(
        background = 0xF8F3EA,
        text = 0x342B23,
        muted = 0x524133,
        accent = 0x5D3D23,
        next = 0xEFE0C8,
        nextText = 0x4C3824,
        card = 0xFFFCF6,
        empty = 0xF0E9DF,
        spots = listOf(0xDDB579, 0xDFAF99, 0xBDC5A4),
        darkIcons = true,
        error = 0x7F1D16,
        errorContainer = 0xFFE7E3,
        onError = 0xFFFFFF,
        onErrorContainer = 0x61100C),
    ColourTheme.ROSEWOOD to palette(
        background = 0x21181D,
        text = 0xF0E5E9,
        muted = 0xE0CCD6,
        accent = 0xF0C2D1,
        next = 0x785666,
        nextText = 0xF5DEE6,
        card = 0x674D5B,
        empty = 0x5E4653,
        spots = listOf(0x754153, 0x514159, 0x625039),
        darkIcons = false,
        error = 0xFFB4AB,
        errorContainer = 0x5C211D,
        onError = 0x39100B,
        onErrorContainer = 0xFFDED9))

fun ColourTheme.palette(): FixedPalette = palettes.getValue(this)
/** The featured card role preserves Lake's softer emphasis without page-specific theme checks. */
internal fun ColourTheme.emphasisColours(): AppCardColours {
    val scheme = palette().scheme
    return if (this == ColourTheme.LAKE) AppCardColours(scheme.primaryContainer, scheme.onPrimaryContainer)
        else AppCardColours(scheme.primary, scheme.onPrimary)
}

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
