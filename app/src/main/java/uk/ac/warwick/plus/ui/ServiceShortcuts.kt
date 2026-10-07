package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.components.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal fun serviceIcon(name: String, draw: PathBuilder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = draw)
    }.build()

internal object ServiceIcons {
    val moodle = serviceIcon("Learning") {
        moveTo(2f, 8f); lineTo(12f, 3f); lineTo(22f, 8f); lineTo(12f, 13f); close()
        moveTo(6f, 10f); lineTo(6f, 17f); quadTo(12f, 22f, 18f, 17f); lineTo(18f, 10f)
        moveTo(22f, 8f); lineTo(22f, 16f)
    }
    val email = serviceIcon("Mail") {
        moveTo(5f, 5f); lineTo(19f, 5f); quadTo(21f, 5f, 21f, 7f)
        lineTo(21f, 17f); quadTo(21f, 19f, 19f, 19f); lineTo(5f, 19f)
        quadTo(3f, 19f, 3f, 17f); lineTo(3f, 7f); quadTo(3f, 5f, 5f, 5f); close()
        moveTo(3f, 7f); lineTo(12f, 13f); lineTo(21f, 7f)
    }
    val tabula = serviceIcon("StudentRecord") {
        moveTo(7f, 3f); lineTo(17f, 3f); quadTo(19f, 3f, 19f, 5f)
        lineTo(19f, 19f); quadTo(19f, 21f, 17f, 21f); lineTo(7f, 21f)
        quadTo(5f, 21f, 5f, 19f); lineTo(5f, 5f); quadTo(5f, 3f, 7f, 3f); close()
        moveTo(9f, 8f); lineTo(15f, 8f); moveTo(9f, 12f); lineTo(15f, 12f)
        moveTo(9f, 16f); lineTo(13f, 16f)
    }
    val library = serviceIcon("Books") {
        moveTo(3f, 4f); lineTo(7f, 4f); lineTo(7f, 20f); lineTo(3f, 20f); close()
        moveTo(7f, 4f); lineTo(11f, 4f); lineTo(11f, 20f); lineTo(7f, 20f)
        moveTo(14f, 4f); lineTo(18f, 3f); lineTo(22f, 19f); lineTo(18f, 20f); close()
        moveTo(3f, 8f); lineTo(7f, 8f); moveTo(7f, 16f); lineTo(11f, 16f)
    }
}

@Composable
internal fun HomeQuickLinks(onOpen: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().testTag("home-quick-links"), horizontalArrangement = Arrangement.spacedBy(Spacing.grid)) {
        HomeServices.forEach { service ->
            val label = stringResource(requireNotNull(service.homeLabelRes))
            ActionTile(label, service.icon, { onOpen(service.url) },
                layout = ActionTileLayout.Compact,
                modifier = Modifier.weight(1f).testTag("home-service-${service.name.lowercase()}"),
                actionLabel = stringResource(R.string.open_in_browser, stringResource(service.labelRes)))
        }
    }
}
