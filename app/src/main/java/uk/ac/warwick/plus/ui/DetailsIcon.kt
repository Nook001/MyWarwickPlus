package uk.ac.warwick.plus.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val DetailsChevron = ImageVector.Builder("DetailsChevron", 24.dp, 24.dp, 24f, 24f, autoMirror = true).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(9f, 6f); lineTo(7.6f, 7.4f); lineTo(12.2f, 12f)
        lineTo(7.6f, 16.6f); lineTo(9f, 18f); lineTo(15f, 12f); close()
    }
}.build()
