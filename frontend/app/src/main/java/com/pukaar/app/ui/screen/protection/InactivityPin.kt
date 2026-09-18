package com.pukaar.app.ui.screen.protection

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A map pin with a clock face cut into it — the inactivity mark.
 *
 * Material ships no pin-with-clock, and the two halves carry the whole idea:
 * where you are, and how long it has been quiet. Drawn here rather than shipped
 * as an asset so it takes a tint and scales with the other icons.
 *
 * The face is a hole rather than a filled disc — [PathFillType.EvenOdd] on the
 * body path — so the colour behind the icon shows through it.
 */
val InactivityPin: ImageVector by lazy {
    ImageVector.Builder(
        name = "InactivityPin",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd
        ) {
            // The teardrop.
            moveTo(12f, 1.8f)
            curveToRelative(-4.2f, 0f, -8f, 3.3f, -8f, 8.4f)
            curveToRelative(0f, 3.5f, 2.7f, 7.6f, 8f, 12.1f)
            curveToRelative(5.3f, -4.5f, 8f, -8.6f, 8f, -12.1f)
            curveToRelative(0f, -5.1f, -3.8f, -8.4f, -8f, -8.4f)
            close()

            // The face, punched out of it.
            moveTo(7.5f, 10.1f)
            arcToRelative(4.5f, 4.5f, 0f, true, false, 9f, 0f)
            arcToRelative(4.5f, 4.5f, 0f, true, false, -9f, 0f)
            close()
        }

        // Minute hand, straight up from the centre of the face.
        path(fill = SolidColor(Color.White)) {
            moveTo(11.6f, 6.5f)
            horizontalLineToRelative(0.9f)
            verticalLineToRelative(4.1f)
            horizontalLineToRelative(-0.9f)
            close()
        }

        // Hour hand, out to the right.
        path(fill = SolidColor(Color.White)) {
            moveTo(11.6f, 9.7f)
            horizontalLineToRelative(2.7f)
            verticalLineToRelative(0.9f)
            horizontalLineToRelative(-2.7f)
            close()
        }
    }.build()
}
