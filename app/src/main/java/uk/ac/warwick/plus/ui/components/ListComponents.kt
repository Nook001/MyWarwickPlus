package uk.ac.warwick.plus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SectionCard(title: String, modifier: Modifier = Modifier,
    headingTag: String? = null, trailing: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    AppCard(modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().then(if (headingTag == null) Modifier else Modifier.testTag("$headingTag-header"))
                .heightIn(min = 28.dp).padding(horizontal = Spacing.contentInset),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).then(if (headingTag == null) Modifier else Modifier.testTag("$headingTag-heading")))
                trailing()
            }
            content()
        }
    }
}

@Composable
internal fun SectionEmptyRow(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 10.dp))
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
@Composable
internal fun MetricListRow(onSelect: () -> Unit, actionLabel: String, modifier: Modifier = Modifier,
    compactTop: Boolean = false, metric: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    val width = with(LocalDensity.current) { 48.sp.toDp() }
    Row(modifier.fillMaxWidth().clickable(onClickLabel = actionLabel, onClick = onSelect)
        .heightIn(min = if (compactTop) 56.dp else 64.dp)
        .padding(start = 12.dp, end = 12.dp, top = if (compactTop) 2.dp else 10.dp, bottom = if (compactTop) 8.dp else 10.dp),
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
