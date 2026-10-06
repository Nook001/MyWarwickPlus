package uk.ac.warwick.plus.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.ac.warwick.plus.data.*

@Composable
private fun RowScope.CompactTab(label: String, selected: Boolean, onSelect: () -> Unit,
    tag: String = "tab-${label.lowercase()}", icon: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val highlight = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Box(Modifier.weight(1f).selectable(selected, interactionSource = interactionSource, indication = null,
        role = Role.Tab, onClick = onSelect).testTag(tag)
        .heightIn(min = 64.dp).padding(horizontal = 2.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = AppShapes.featured, color = highlight,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
            Column(Modifier.padding(horizontal = 2.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

@Composable
internal fun AppNavigation(tab: Int, needsSignIn: Boolean, logoutFailed: Boolean, onSelect: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = 20.dp).selectableGroup().testTag("compact-tab-bar"), verticalAlignment = Alignment.CenterVertically) {
            CompactTab("Home", tab == 0, { onSelect(0) }) {
                Icon(if (tab == 0) NavigationIcons.homeFilled else NavigationIcons.homeOutline, null, Modifier.size(24.dp))
            }
            CompactTab("Classes", tab == 1, { onSelect(1) }, "schedule-tab") {
                Icon(if (tab == 1) NavigationIcons.scheduleFilled else NavigationIcons.scheduleOutline, null, Modifier.size(24.dp))
            }
            CompactTab("Tasks", tab == 2, { onSelect(2) }, "tab-coursework") {
                Icon(if (tab == 2) NavigationIcons.courseworkFilled else NavigationIcons.courseworkOutline, null, Modifier.size(24.dp))
            }
            CompactTab("Me", tab == 3, { onSelect(3) }, "more-tab") {
                Box(Modifier.size(24.dp)) {
                    Icon(if (tab == 3) NavigationIcons.meFilled else NavigationIcons.meOutline, null, Modifier.size(24.dp))
                    if (needsSignIn || logoutFailed) Box(Modifier.align(Alignment.TopEnd)
                        .size(5.dp).background(MaterialTheme.colorScheme.error, CircleShape).semantics {
                            contentDescription = if (logoutFailed) "Sign-out needs attention" else "Sign in required"
                        })
                }
            }
        }
    }
}
