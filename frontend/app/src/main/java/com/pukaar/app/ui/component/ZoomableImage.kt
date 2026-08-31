package com.pukaar.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import kotlin.math.min

/**
 * An image the user can pinch to zoom and drag to pan.
 *
 * The image starts fitted to the viewport. Zooming keeps whatever is under the
 * pinch anchored, panning is clamped so the image can never be dragged off
 * screen, and a double tap snaps between fitted and [doubleTapScale].
 */
@Composable
fun ZoomableImage(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxScale: Float = 6f,
    doubleTapScale: Float = 2.5f
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val viewportWidth = constraints.maxWidth.toFloat()
        val viewportHeight = constraints.maxHeight.toFloat()

        // How large the image is drawn at scale 1, i.e. fitted to the viewport.
        val intrinsic = painter.intrinsicSize
        val fitScale = if (intrinsic.width > 0f && intrinsic.height > 0f) {
            min(viewportWidth / intrinsic.width, viewportHeight / intrinsic.height)
        } else {
            1f
        }
        val fittedWidth = intrinsic.width * fitScale
        val fittedHeight = intrinsic.height * fitScale

        /** Keeps the image covering the viewport — no dragging into empty space. */
        fun clampOffset(candidate: Offset, atScale: Float): Offset {
            val maxX = ((fittedWidth * atScale - viewportWidth) / 2f).coerceAtLeast(0f)
            val maxY = ((fittedHeight * atScale - viewportHeight) / 2f).coerceAtLeast(0f)
            return Offset(
                x = candidate.x.coerceIn(-maxX, maxX),
                y = candidate.y.coerceIn(-maxY, maxY)
            )
        }

        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, maxScale)

                        // Hold the content under the pinch centroid in place.
                        val focus = centroid - Offset(viewportWidth / 2f, viewportHeight / 2f)
                        val rescaled = focus - (focus - offset) * (newScale / scale)

                        scale = newScale
                        offset = clampOffset(rescaled + pan, newScale)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            val target = if (scale > 1.01f) 1f else doubleTapScale
                            scale = target
                            offset = clampOffset(Offset.Zero, target)
                        }
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
