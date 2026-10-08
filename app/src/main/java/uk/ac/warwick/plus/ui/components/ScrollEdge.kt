package uk.ac.warwick.plus.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The page background is a bitmap, so the edge masks content alpha instead of painting a colour over it. */
internal fun Modifier.fadingTopEdge(state: LazyListState, height: Dp = 16.dp) =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (state.canScrollBackward) {
                val edge = height.toPx()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, endY = edge),
                    size = Size(size.width, edge), blendMode = BlendMode.DstIn)
            }
        }
