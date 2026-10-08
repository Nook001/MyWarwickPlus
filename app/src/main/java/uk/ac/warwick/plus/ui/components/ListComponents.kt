package uk.ac.warwick.plus.ui.components

import uk.ac.warwick.plus.ui.DetailsChevron
import uk.ac.warwick.plus.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SectionCard(title: String, modifier: Modifier = Modifier,
    headingTag: String? = null, icon: ImageVector? = null, tone: CardTone = CardTone.Normal,
    trailing: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    AppCard(modifier.fillMaxWidth(), tone = tone) {
        Column {
            Row(Modifier.fillMaxWidth().then(if (headingTag == null) Modifier else Modifier.testTag("$headingTag-header"))
                .padding(Spacing.sectionHeader).heightIn(min = Spacing.sectionHeadingHeight),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).then(if (headingTag == null) Modifier else Modifier.testTag("$headingTag-heading")))
                trailing()
            }
            content()
        }
    }
}

@Composable
internal fun SectionAllAction(actionLabel: String, onClick: () -> Unit) {
    Row(Modifier.widthIn(min = 48.dp).heightIn(min = Spacing.sectionHeadingHeight)
        .clickable(onClickLabel = actionLabel, role = Role.Button, onClick = onClick)
        .padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End) {
        Text(stringResource(R.string.action_all),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Icon(DetailsChevron, contentDescription = null, modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun SectionEmptyRow(text: String, modifier: Modifier = Modifier) {
    Text(text, style = HomeTypography.content, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp))
}

@Composable
internal fun ListDivider(inset: Dp = Spacing.contentInset) = HorizontalDivider(Modifier.padding(horizontal = inset),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))

/** Keeps lazy item keys/virtualization with one visual background for the whole list. */
@Composable
internal fun GroupedListItem(first: Boolean, last: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val top = if (first) AppShapes.sectionRadius else 0.dp
    val bottom = if (last) AppShapes.sectionRadius else 0.dp
    AppCard(shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)) {
        Column {
            content()
            if (!last) ListDivider()
        }
    }
}

/** Shared geometry only; class status and deadline rules belong to their domain rows. */
internal enum class ListRowDensity { Standard, Compact }

@Composable
internal fun MetricListRow(onSelect: () -> Unit, actionLabel: String, modifier: Modifier = Modifier,
    compactTop: Boolean = false, density: ListRowDensity = ListRowDensity.Standard,
    metric: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    val compact = density == ListRowDensity.Compact
    val rowPadding = if (compact) Spacing.compactRowPadding else 10.dp
    val width = with(LocalDensity.current) { (if (compact) 40.sp else 48.sp).toDp() }
    Row(modifier.fillMaxWidth().clickable(onClickLabel = actionLabel, onClick = onSelect)
        .heightIn(min = if (compactTop) Spacing.sectionFirstRowHeight else if (compact) Spacing.compactRowHeight else 64.dp)
        .padding(start = 12.dp, end = 12.dp, top = if (compactTop) 0.dp else rowPadding,
            bottom = if (compactTop) 8.dp else rowPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp), content = metric)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
        DetailsArrow()
    }
}

@Composable
internal fun DetailsArrow(modifier: Modifier = Modifier, size: Dp = 16.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) =
    Icon(DetailsChevron, contentDescription = null, modifier = modifier.size(size), tint = tint)
