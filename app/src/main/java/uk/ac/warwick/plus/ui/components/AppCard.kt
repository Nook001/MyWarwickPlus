package uk.ac.warwick.plus.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics

internal enum class CardTone { Normal, Quiet, Featured, Selected }
internal data class AppCardColours(val background: Color, val foreground: Color)

@Composable
internal fun appCardColours(tone: CardTone): AppCardColours {
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        CardTone.Normal -> AppCardColours(scheme.surfaceContainerLow, scheme.onSurface)
        CardTone.Quiet -> AppCardColours(scheme.surfaceContainer, scheme.onSurface)
        CardTone.Featured -> LocalAppearance.current.theme.emphasisColours()
        CardTone.Selected -> AppCardColours(scheme.primaryContainer, scheme.onPrimaryContainer)
    }
}

/** No implicit padding or outside margin: content components own their layout. */
@Composable
internal fun AppCard(modifier: Modifier = Modifier, shape: Shape = AppShapes.section,
    tone: CardTone = CardTone.Normal, content: @Composable () -> Unit) {
    val colours = appCardColours(tone)
    Surface(modifier, shape = shape, color = colours.background, contentColor = colours.foreground,
        content = content)
}

@Composable
internal fun AppCard(onClick: () -> Unit, modifier: Modifier = Modifier, shape: Shape = AppShapes.section,
    tone: CardTone = CardTone.Normal, actionLabel: String? = null, content: @Composable () -> Unit) {
    val colours = appCardColours(tone)
    val semantics = if (actionLabel == null) modifier else modifier.semantics { onClick(label = actionLabel, action = null) }
    Surface(onClick = onClick, modifier = semantics, shape = shape, color = colours.background,
        contentColor = colours.foreground, content = content)
}
