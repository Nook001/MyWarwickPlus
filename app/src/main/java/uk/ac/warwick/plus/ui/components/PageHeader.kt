package uk.ac.warwick.plus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun AppPageHeader(title: String, subtitle: String, modifier: Modifier = Modifier,
    titleTag: String? = null,
    actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().testTag("page-header").padding(horizontal = 16.dp, vertical = 2.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold, modifier = titleTag?.let { Modifier.testTag(it) } ?: Modifier)
        }
        actions()
    }
}
