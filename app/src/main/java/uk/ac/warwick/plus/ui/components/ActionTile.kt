package uk.ac.warwick.plus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class ActionTileLayout { Compact, Grid }
internal enum class ActionDestination { Internal, External }

@Composable
internal fun ActionTile(label: String, icon: ImageVector, onClick: () -> Unit,
    layout: ActionTileLayout, modifier: Modifier = Modifier, minimumHeight: Dp = 64.dp,
    destination: ActionDestination? = null, actionLabel: String = "Open $label") {
    val compact = layout == ActionTileLayout.Compact
    AppCard(onClick, modifier, shape = if (compact) AppShapes.section else AppShapes.tile, actionLabel = actionLabel) {
        Box(Modifier.heightIn(min = minimumHeight)) {
            Column(Modifier.align(Alignment.Center).then(if (compact)
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp) else Modifier.padding(10.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp, Alignment.CenterVertically)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) 20.dp else 24.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text(label, style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
            }
            if (destination != null) Icon(if (destination == ActionDestination.External) MeIcons.external else DetailsChevron,
                contentDescription = null, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
