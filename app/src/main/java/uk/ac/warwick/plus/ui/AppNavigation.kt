package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R

import androidx.compose.ui.res.stringResource

import uk.ac.warwick.plus.ui.components.*

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.ac.warwick.plus.data.*

@Composable
private fun RowScope.CompactTab(label: String, selected: Boolean, onSelect: () -> Unit,
    tag: String, icon: @Composable () -> Unit) {
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
internal fun AppNavigation(tab: AppTab, needsSignIn: Boolean, logoutFailed: Boolean, onSelect: (AppTab) -> Unit) {
    val attention = stringResource(if (logoutFailed) R.string.sign_out_attention else R.string.sign_in_required)
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = 20.dp).selectableGroup().testTag("compact-tab-bar"), verticalAlignment = Alignment.CenterVertically) {
            AppTab.entries.forEach { item ->
                val selected = tab == item
                CompactTab(stringResource(item.labelRes), selected, { onSelect(item) }, item.tag) {
                    Box(Modifier.size(24.dp)) {
                        val icon = when (item) {
                            AppTab.HOME -> if (selected) NavigationIcons.homeFilled else NavigationIcons.homeOutline
                            AppTab.CLASSES -> if (selected) NavigationIcons.scheduleFilled else NavigationIcons.scheduleOutline
                            AppTab.TASKS -> if (selected) NavigationIcons.courseworkFilled else NavigationIcons.courseworkOutline
                            AppTab.ME -> if (selected) NavigationIcons.meFilled else NavigationIcons.meOutline
                        }
                        Icon(icon, null, Modifier.size(24.dp))
                        if (item == AppTab.ME && (needsSignIn || logoutFailed)) Box(Modifier.align(Alignment.TopEnd)
                            .size(5.dp).background(MaterialTheme.colorScheme.error, CircleShape).semantics {
                                contentDescription = attention
                            })
                    }
                }
            }
        }
    }
}
