package uk.ac.warwick.plus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.ui.ContentIcons
import uk.ac.warwick.plus.R

/** Location metadata, not a GPS action; the enclosing row owns navigation. */
@Composable
internal fun LocationLabel(location: String, modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    colour: Color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines: Int = 1) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (location.isNotBlank()) Icon(ContentIcons.location, contentDescription = null,
            modifier = Modifier.size(14.dp), tint = colour)
        Text(location.ifBlank { stringResource(R.string.location_not_provided) },
            style = style, color = colour, maxLines = maxLines,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
    }
}
