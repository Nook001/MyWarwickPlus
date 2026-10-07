package uk.ac.warwick.plus.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Tokens describe existing layouts; parents own spacing between components. */
internal object AppShapes {
    val sectionRadius = 14.dp
    val section = RoundedCornerShape(sectionRadius)
    val tile = RoundedCornerShape(16.dp)
    val featured = RoundedCornerShape(18.dp)
    val feedContent = RoundedCornerShape(20.dp)
    val search = RoundedCornerShape(24.dp)
}

object Spacing {
    val page = 20.dp
    val item = 12.dp
    val homeSection = 24.dp
    val grid = 8.dp
    val contentInset = 12.dp
    val sectionHeader = PaddingValues(start = contentInset, end = contentInset, top = 8.dp, bottom = 2.dp)
    val sectionHeadingHeight = 20.dp
    val sectionFirstRowHeight = 48.dp
    val compactPage = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
    val detailPage = PaddingValues(24.dp)
}
