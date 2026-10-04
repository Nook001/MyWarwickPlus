package uk.ac.warwick.plus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun AppearanceContent() {
    val appearance = LocalAppearance.current
    val update = LocalAppearanceChange.current
    LazyColumn(Modifier.fillMaxSize().testTag("appearance-list").selectableGroup(),
        contentPadding = PaddingValues(Spacing.page), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionLabel("COLOUR THEME") }
        items(ColourTheme.entries, key = { it.id }) { theme ->
            val selected = appearance.theme == theme
            Surface(shape = RoundedCornerShape(18.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) {
                Row(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton,
                    onClick = { update(appearance.copy(theme = theme)) }).testTag("theme-${theme.id}")
                    .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(28.dp).background(theme.palette().next, CircleShape))
                    Column(Modifier.weight(1f)) {
                        Text(theme.label, style = MaterialTheme.typography.titleMedium)
                        Text(theme.description, style = MaterialTheme.typography.bodySmall)
                    }
                    RadioButton(selected, onClick = null)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().toggleable(appearance.texture, role = Role.Switch,
                    onValueChange = { update(appearance.copy(texture = it)) }).testTag("fine-texture")
                    .padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Fine texture", style = MaterialTheme.typography.titleMedium)
                        Text("A subtle frosted finish", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(appearance.texture, onCheckedChange = null)
                }
            }
        }
    }
}
