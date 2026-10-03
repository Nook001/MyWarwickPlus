package uk.ac.warwick.plus.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF315D4E), onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EDE5), onPrimaryContainer = Color(0xFF16392B),
    secondary = Color(0xFF496556), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE8DC), onSecondaryContainer = Color(0xFF1B3828),
    background = Color(0xFFF8F9F5), surface = Color(0xFFF8F9F5),
    surfaceContainer = Color(0xFFF0F2ED), surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFE8EEE6), surfaceContainerHighest = Color(0xFFE0E8DE),
    onSurface = Color(0xFF1B2822), onSurfaceVariant = Color(0xFF59635D),
    outlineVariant = Color(0xFFDCE2DA)
)
private val Dark = darkColorScheme(primary = Color(0xFFA1D0B5),
    background = Color(0xFF111A15), surface = Color(0xFF111A15),
    primaryContainer = Color(0xFF244535), onPrimaryContainer = Color(0xFFD7EBDD),
    secondary = Color(0xFFB0CCB8), secondaryContainer = Color(0xFF304C3A),
    onSecondaryContainer = Color(0xFFD7EBDD),
    surfaceContainerLow = Color(0xFF19231D), surfaceContainer = Color(0xFF202D25),
    surfaceContainerHigh = Color(0xFF28352D), surfaceContainerHighest = Color(0xFF303D35),
    onSurface = Color(0xFFE2EBE4), onSurfaceVariant = Color(0xFFADB9AF), outlineVariant = Color(0xFF39473D))

@Composable
fun PlusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
