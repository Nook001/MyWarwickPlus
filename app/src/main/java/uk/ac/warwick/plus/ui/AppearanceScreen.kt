package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.components.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AppearanceContent() {
    val appearance = LocalAppearance.current
    val update = LocalAppearanceChange.current
    LazyColumn(Modifier.fillMaxSize().testTag("appearance-list"),
        contentPadding = PaddingValues(Spacing.page), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "appearance") {
            SectionCard(stringResource(R.string.appearance_section)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.contentInset, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.colour_theme), style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                    Text(stringResource(appearance.theme.descriptionRes), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 4.dp, vertical = 4.dp)) {
                    ColourTheme.entries.forEach { theme ->
                        ThemeSwatch(theme, appearance.theme == theme, Modifier.weight(1f)) { update(appearance.copy(theme = theme)) }
                    }
                }
                LocalLanguage.current?.let { LanguageChoice(it) }
                ListDivider()
                SettingSwitch(stringResource(R.string.fine_texture), stringResource(R.string.fine_texture_detail),
                    appearance.texture, "fine-texture") { update(appearance.copy(texture = it)) }
            }
        }
        reminderSettings()
        widgetSettings()
        updateSettings()
    }
}

@Composable
private fun LanguageChoice(language: LanguagePreference) {
    val activity = LocalActivity.current
    val current = remember(language) { language.current() }
    ListDivider()
    Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = Spacing.contentInset, end = Spacing.contentInset, top = 10.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.contentInset, vertical = 8.dp)) {
        AppLanguage.entries.forEachIndexed { index, option ->
            SegmentedButton(selected = option == current, modifier = Modifier.testTag("language-${option.name.lowercase()}"),
                onClick = { if (option != current && activity != null) language.select(option, activity) },
                shape = SegmentedButtonDefaults.itemShape(index, AppLanguage.entries.size),
                label = { Text(stringResource(option.labelRes), maxLines = 1) })
        }
    }
}

@Composable
private fun ThemeSwatch(theme: ColourTheme, selected: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    val palette = theme.palette()
    Column(modifier.selectable(selected, role = Role.RadioButton, onClick = onSelect).testTag("theme-${theme.id}")
        .padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // The ring keeps a swatch visible when it matches the card colour.
        Box(Modifier.size(40.dp).border(if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(4.dp).background(palette.next, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Icon(MeIcons.check, contentDescription = null, modifier = Modifier.size(18.dp), tint = palette.nextText)
        }
        Text(stringResource(theme.labelRes), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}
