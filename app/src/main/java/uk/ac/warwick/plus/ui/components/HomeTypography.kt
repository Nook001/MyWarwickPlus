package uk.ac.warwick.plus.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Compact home content stays at the section-label size; system font scaling remains active. */
internal object HomeTypography {
    val content
        @Composable get() = MaterialTheme.typography.bodySmall
    val metric
        @Composable get() = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val metricSecondary
        @Composable get() = metric.copy(fontWeight = FontWeight.Normal)
}
