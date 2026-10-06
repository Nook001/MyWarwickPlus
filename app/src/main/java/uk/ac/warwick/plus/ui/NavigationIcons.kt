package uk.ac.warwick.plus.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun navigationIcon(name: String, filled: Boolean, draw: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = if (filled) SolidColor(Color.Black) else null,
            stroke = if (filled) null else SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.EvenOdd, pathBuilder = draw)
    }.build()

internal object NavigationIcons {
    val homeOutline = navigationIcon("HomeOutline", false) {
        moveTo(3f, 10f); lineTo(12f, 3f); lineTo(21f, 10f)
        moveTo(5f, 8.5f); lineTo(5f, 21f); lineTo(10f, 21f); lineTo(10f, 14f)
        lineTo(14f, 14f); lineTo(14f, 21f); lineTo(19f, 21f); lineTo(19f, 8.5f)
    }
    val homeFilled = navigationIcon("HomeFilled", true) {
        moveTo(2f, 10f); lineTo(12f, 2f); lineTo(22f, 10f); lineTo(20f, 10f)
        lineTo(20f, 22f); lineTo(4f, 22f); lineTo(4f, 10f); close()
        moveTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 22f); lineTo(10f, 22f); close()
    }

    val scheduleOutline = navigationIcon("ScheduleOutline", false) {
        moveTo(5f, 5f); lineTo(19f, 5f); quadTo(21f, 5f, 21f, 7f)
        lineTo(21f, 19f); quadTo(21f, 21f, 19f, 21f)
        lineTo(5f, 21f); quadTo(3f, 21f, 3f, 19f)
        lineTo(3f, 7f); quadTo(3f, 5f, 5f, 5f); close()
        moveTo(7f, 3f); lineTo(7f, 7f); moveTo(17f, 3f); lineTo(17f, 7f)
        moveTo(3f, 10f); lineTo(21f, 10f)
        moveTo(7f, 14f); lineTo(8f, 14f); moveTo(12f, 14f); lineTo(13f, 14f)
        moveTo(17f, 14f); lineTo(18f, 14f); moveTo(7f, 18f); lineTo(8f, 18f)
        moveTo(12f, 18f); lineTo(13f, 18f)
    }
    val scheduleFilled = navigationIcon("ScheduleFilled", true) {
        moveTo(5f, 5f); lineTo(6f, 5f); lineTo(6f, 2f); lineTo(8f, 2f); lineTo(8f, 5f)
        lineTo(16f, 5f); lineTo(16f, 2f); lineTo(18f, 2f); lineTo(18f, 5f); lineTo(19f, 5f)
        quadTo(22f, 5f, 22f, 8f); lineTo(22f, 19f); quadTo(22f, 22f, 19f, 22f)
        lineTo(5f, 22f); quadTo(2f, 22f, 2f, 19f); lineTo(2f, 8f)
        quadTo(2f, 5f, 5f, 5f); close()
        moveTo(4f, 9f); lineTo(20f, 9f); lineTo(20f, 10.5f); lineTo(4f, 10.5f); close()
        for (x in listOf(6f, 11f, 16f)) {
            moveTo(x, 13f); lineTo(x + 2f, 13f); lineTo(x + 2f, 15f); lineTo(x, 15f); close()
        }
        for (x in listOf(6f, 11f)) {
            moveTo(x, 17f); lineTo(x + 2f, 17f); lineTo(x + 2f, 19f); lineTo(x, 19f); close()
        }
    }

    val courseworkOutline = navigationIcon("CourseworkOutline", false) {
        moveTo(8f, 5f); lineTo(5f, 5f); lineTo(5f, 22f); lineTo(19f, 22f); lineTo(19f, 5f); lineTo(16f, 5f)
        moveTo(8f, 3f); lineTo(16f, 3f); lineTo(16f, 7f); lineTo(8f, 7f); close()
        moveTo(8f, 12f); lineTo(9f, 13f); lineTo(11f, 11f); moveTo(14f, 12f); lineTo(16f, 12f)
        moveTo(8f, 17f); lineTo(9f, 18f); lineTo(11f, 16f); moveTo(14f, 17f); lineTo(16f, 17f)
    }
    val courseworkFilled = navigationIcon("CourseworkFilled", true) {
        moveTo(5f, 4f); lineTo(7f, 4f); lineTo(7f, 2f); lineTo(17f, 2f); lineTo(17f, 4f)
        lineTo(19f, 4f); quadTo(21f, 4f, 21f, 6f); lineTo(21f, 21f)
        quadTo(21f, 23f, 19f, 23f); lineTo(5f, 23f); quadTo(3f, 23f, 3f, 21f)
        lineTo(3f, 6f); quadTo(3f, 4f, 5f, 4f); close()
        moveTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 6f); lineTo(9f, 6f); close()
        for (y in listOf(12f, 17f)) {
            moveTo(7f, y); lineTo(8.5f, y + 1.5f); lineTo(11.5f, y - 1.5f)
            lineTo(10.5f, y - 2.5f); lineTo(8.5f, y - .5f); lineTo(8f, y - 1f); close()
            moveTo(14f, y - 1f); lineTo(18f, y - 1f); lineTo(18f, y + .5f); lineTo(14f, y + .5f); close()
        }
    }

    val meOutline = navigationIcon("MeOutline", false) {
        moveTo(16f, 7f); curveTo(16f, 9.2f, 14.2f, 11f, 12f, 11f)
        curveTo(9.8f, 11f, 8f, 9.2f, 8f, 7f); curveTo(8f, 4.8f, 9.8f, 3f, 12f, 3f)
        curveTo(14.2f, 3f, 16f, 4.8f, 16f, 7f); close()
        moveTo(4f, 21f); lineTo(4f, 19f); curveTo(4f, 15.5f, 7.6f, 14f, 12f, 14f)
        curveTo(16.4f, 14f, 20f, 15.5f, 20f, 19f); lineTo(20f, 21f); close()
    }
    val meFilled = navigationIcon("MeFilled", true) {
        moveTo(16.5f, 7f); curveTo(16.5f, 9.5f, 14.5f, 11.5f, 12f, 11.5f)
        curveTo(9.5f, 11.5f, 7.5f, 9.5f, 7.5f, 7f); curveTo(7.5f, 4.5f, 9.5f, 2.5f, 12f, 2.5f)
        curveTo(14.5f, 2.5f, 16.5f, 4.5f, 16.5f, 7f); close()
        moveTo(3f, 22f); lineTo(3f, 19f); curveTo(3f, 15f, 7f, 13f, 12f, 13f)
        curveTo(17f, 13f, 21f, 15f, 21f, 19f); lineTo(21f, 22f); close()
    }
}
