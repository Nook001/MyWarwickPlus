package uk.ac.warwick.plus

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.ui.*
import kotlin.math.pow

class AppearancePaletteTest {
    private fun luminance(colour: Color): Double {
        fun linear(value: Float) = if (value <= .04045f) value / 12.92 else ((value + .055) / 1.055).pow(2.4)
        return .2126 * linear(colour.red) + .7152 * linear(colour.green) + .0722 * linear(colour.blue)
    }
    private fun contrast(a: Color, b: Color): Double {
        val x = luminance(a); val y = luminance(b)
        return (maxOf(x,y) + .05) / (minOf(x,y) + .05)
    }
    @Test fun allFixedPalettesKeepReadableMaterialForegrounds() {
        ColourTheme.entries.forEach { theme ->
            val s = theme.palette().scheme
            listOf(s.primary to s.onPrimary, s.primaryContainer to s.onPrimaryContainer,
                s.secondary to s.onSecondary, s.secondaryContainer to s.onSecondaryContainer,
                s.tertiary to s.onTertiary, s.tertiaryContainer to s.onTertiaryContainer,
                s.surface to s.onSurface, s.surfaceContainerLow to s.onSurfaceVariant,
                s.surfaceContainer to s.onSurfaceVariant, s.inverseSurface to s.inverseOnSurface,
                s.error to s.onError, s.errorContainer to s.onErrorContainer,
                s.primaryFixed to s.onPrimaryFixed, s.secondaryFixed to s.onSecondaryFixed,
                s.tertiaryFixed to s.onTertiaryFixed).forEach { (background, foreground) ->
                assertTrue("${theme.id} foreground contrast must be at least 4.5", contrast(background, foreground) >= 4.5)
            }
        }
    }
    @Test fun unknownPreferenceFallsBackToForestAndIdsAreUnique() {
        assertEquals(ColourTheme.FOREST, ColourTheme.fromId("removed-theme"))
        assertEquals(ColourTheme.FOREST, ColourTheme.fromId(null))
        assertEquals(5, ColourTheme.entries.map { it.id }.distinct().size)
    }
    @Test fun backgroundDimensionsRemainBoundedAndSeparateTextureAndOrientation() {
        val portrait = backgroundKey(Appearance(), 1080f / 2400)
        assertEquals(1024, portrait.height); assertTrue(portrait.width in 448..480)
        assertTrue(portrait.width * portrait.height * 4 < 2 * 1024 * 1024)
        val landscape = backgroundKey(Appearance(), 2400f / 1080)
        assertEquals(1024, landscape.width); assertTrue(landscape.height in 448..480)
        assertNotEquals(portrait, backgroundKey(Appearance(texture = true), 1080f / 2400))
        assertNotEquals(portrait, landscape)
    }
}
