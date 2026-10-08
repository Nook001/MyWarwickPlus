package uk.ac.warwick.plus.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Message previews are compact; full bodies keep a larger reading size. */
internal object InboxTypography {
    val title
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
    val preview
        @Composable get() = MaterialTheme.typography.bodySmall
    val metadata
        @Composable get() = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal)
    val detailTitle
        @Composable get() = MaterialTheme.typography.titleMedium.copy(
            fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val detailBody
        @Composable get() = MaterialTheme.typography.bodyMedium
}
