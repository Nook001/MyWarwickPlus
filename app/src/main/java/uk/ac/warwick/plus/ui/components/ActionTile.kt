package uk.ac.warwick.plus.ui.components

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.MeIcons
import uk.ac.warwick.plus.ui.DetailsChevron
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class ActionTileLayout { Square, Grid }
internal enum class ActionDestination { Internal, External }

@Composable
internal fun ActionTile(label: String, icon: ImageVector, onClick: () -> Unit,
    layout: ActionTileLayout, modifier: Modifier = Modifier,
    minimumHeight: Dp = if (layout == ActionTileLayout.Square) 48.dp else 72.dp,
    destination: ActionDestination? = null, actionLabel: String? = null) {
    val compact = layout == ActionTileLayout.Square
    AppCard(onClick, modifier, shape = if (compact) AppShapes.section else AppShapes.tile,
        tone = if (compact) CardTone.Quiet else CardTone.Normal,
        actionLabel = actionLabel ?: stringResource(R.string.open_item, label)) {
        Box(Modifier.fillMaxWidth().heightIn(min = minimumHeight)) {
            Column(Modifier.align(Alignment.Center).then(if (compact)
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp) else Modifier.padding(horizontal = 6.dp, vertical = 8.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) 20.dp else 22.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text(label, style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
            }
            if (destination != null) Icon(if (destination == ActionDestination.External) MeIcons.external else DetailsChevron,
                contentDescription = null, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(10.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
