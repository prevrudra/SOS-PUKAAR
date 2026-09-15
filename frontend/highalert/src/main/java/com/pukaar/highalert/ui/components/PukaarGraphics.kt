package com.pukaar.highalert.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.highalert.data.EmergencyServiceKind
import com.pukaar.highalert.ui.theme.CallGreen
import com.pukaar.highalert.ui.theme.InfoBlue
import com.pukaar.highalert.ui.theme.MapLand
import com.pukaar.highalert.ui.theme.MapRoad
import com.pukaar.highalert.ui.theme.MapWater
import com.pukaar.highalert.ui.theme.PukaarRed
import com.pukaar.highalert.ui.theme.SurfaceWhite
import com.pukaar.highalert.ui.theme.TextPrimary

/**
 * The PUKAAR bell mark: a ringing bell flanked by sound waves. Drawn rather than
 * shipped as an asset so it scales cleanly at every size the app uses it.
 */
@Composable
fun BellIcon(
    modifier: Modifier = Modifier,
    color: Color = PukaarRed
) {
    Canvas(modifier = modifier) {
        val u = size.minDimension / 100f
        fun x(v: Float) = v * u + (size.width - 100f * u) / 2f
        fun y(v: Float) = v * u + (size.height - 100f * u) / 2f

        // Bell body
        val body = Path().apply {
            moveTo(x(32f), y(64f))
            cubicTo(x(33f), y(42f), x(38f), y(24f), x(50f), y(24f))
            cubicTo(x(62f), y(24f), x(67f), y(42f), x(68f), y(64f))
            close()
        }
        drawPath(body, color)

        // Top knob, rim and clapper
        drawCircle(color, radius = 5.5f * u, center = Offset(x(50f), y(17f)))
        drawRoundRect(
            color = color,
            topLeft = Offset(x(27f), y(62f)),
            size = Size(46f * u, 9.5f * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.5f * u)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(x(44.5f), y(72f)),
            size = Size(11f * u, 11f * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.5f * u)
        )

        // Sound waves on both sides
        val waveCentre = Offset(x(50f), y(46f))
        listOf(30f, 41f).forEach { radius ->
            listOf(150f, -30f).forEach { start ->
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(
                        waveCentre.x - radius * u,
                        waveCentre.y - radius * u
                    ),
                    size = Size(radius * 2 * u, radius * 2 * u),
                    style = Stroke(width = 6f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Wide ringing bell for the alert banner: a flared bell with two sound waves on each side.
 * Laid out on a 180 x 100 grid, centred in whatever box it is given.
 */
@Composable
fun AlertBellIcon(
    modifier: Modifier = Modifier,
    color: Color = PukaarRed
) {
    Canvas(modifier = modifier) {
        val u = minOf(size.width / 180f, size.height / 100f)
        val left = (size.width - 180f * u) / 2f
        val top = (size.height - 100f * u) / 2f
        fun p(x: Float, y: Float) = Offset(left + x * u, top + y * u)

        val body = Path().apply {
            moveTo(p(44f, 78f))
            cubicTo(p(56f, 72f), p(60f, 60f), p(60f, 46f))
            cubicTo(p(60f, 24f), p(72f, 13f), p(90f, 13f))
            cubicTo(p(108f, 13f), p(120f, 24f), p(120f, 46f))
            cubicTo(p(120f, 60f), p(124f, 72f), p(136f, 78f))
            close()
        }
        drawPath(body, color)
        drawCircle(color, radius = 6.5f * u, center = p(90f, 9f))
        drawRoundRect(
            color = color,
            topLeft = p(38f, 74f),
            size = Size(104f * u, 11f * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.5f * u)
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = p(79f, 78f),
            size = Size(22f * u, 20f * u)
        )

        // Sound waves: inner and outer arc on each side, kept short so they clear the rim
        val centre = p(90f, 48f)
        listOf(58f to 22f, 80f to 24f).forEach { (radius, halfSweep) ->
            listOf(180f, 0f).forEach { mid ->
                drawArc(
                    color = color,
                    startAngle = mid - halfSweep,
                    sweepAngle = halfSweep * 2,
                    useCenter = false,
                    topLeft = Offset(centre.x - radius * u, centre.y - radius * u),
                    size = Size(radius * 2 * u, radius * 2 * u),
                    style = Stroke(width = 9f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
    }
}

/** Speech-bubble mark used by the "High Alert Tone with a Message" row. */
@Composable
fun MessageBubbleIcon(
    modifier: Modifier = Modifier,
    color: Color = InfoBlue
) {
    Canvas(modifier = modifier) {
        val u = size.minDimension / 100f
        val bubble = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 8f * u,
                    top = 14f * u,
                    right = 92f * u,
                    bottom = 74f * u,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f * u)
                )
            )
            moveTo(28f * u, 70f * u)
            lineTo(28f * u, 92f * u)
            lineTo(50f * u, 70f * u)
            close()
        }
        drawPath(bubble, color)
        listOf(30f, 50f, 70f).forEach { cx ->
            drawCircle(SurfaceWhite, radius = 6f * u, center = Offset(cx * u, 44f * u))
        }
    }
}

/** The red PUKAAR app tile shown on the "Explore Main PUKAAR" row. */
@Composable
fun PukaarAppTile(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PukaarRed),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Canvas(modifier = Modifier.size(26.dp)) {
                val u = size.minDimension / 100f
                // Three people
                drawCircle(SurfaceWhite, 11f * u, Offset(50f * u, 28f * u))
                drawCircle(SurfaceWhite, 8f * u, Offset(24f * u, 40f * u))
                drawCircle(SurfaceWhite, 8f * u, Offset(76f * u, 40f * u))
                val shoulders = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = 30f * u, top = 44f * u, right = 70f * u, bottom = 72f * u,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f * u)
                        )
                    )
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = 8f * u, top = 54f * u, right = 36f * u, bottom = 74f * u,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * u)
                        )
                    )
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = 64f * u, top = 54f * u, right = 92f * u, bottom = 74f * u,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * u)
                        )
                    )
                }
                drawPath(shoulders, SurfaceWhite)
                listOf(20f, 30f).forEach { radius ->
                    listOf(150f, -30f).forEach { start ->
                        drawArc(
                            color = SurfaceWhite,
                            startAngle = start,
                            sweepAngle = 60f,
                            useCenter = false,
                            topLeft = Offset(50f * u - radius * u, 40f * u - radius * u),
                            size = Size(radius * 2 * u, radius * 2 * u),
                            style = Stroke(width = 5f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                }
            }
            Text(
                text = "PUKAAR",
                color = SurfaceWhite,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

/** Glyph for police (officer cap) / ambulance (red cross) / hospital (building) columns. */
@Composable
fun EmergencyServiceGlyph(
    kind: EmergencyServiceKind,
    modifier: Modifier = Modifier,
    blue: Color = InfoBlue,
    red: Color = PukaarRed
) {
    Canvas(modifier = modifier) {
        val u = size.minDimension / 100f
        val ox = (size.width - 100f * u) / 2f
        val oy = (size.height - 100f * u) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)
        fun corner(r: Float) = androidx.compose.ui.geometry.CornerRadius(r * u)

        when (kind) {
            EmergencyServiceKind.POLICE -> {
                // Crown of the cap, wider at the top and tapering to the band
                val crown = Path().apply {
                    moveTo(p(8f, 34f))
                    cubicTo(p(20f, 10f), p(80f, 10f), p(92f, 34f))
                    lineTo(p(80f, 58f))
                    lineTo(p(20f, 58f))
                    close()
                }
                drawPath(crown, blue)
                drawCircle(SurfaceWhite, radius = 8f * u, center = p(50f, 36f))
                drawRoundRect(blue, topLeft = p(18f, 62f), size = Size(64f * u, 10f * u), cornerRadius = corner(3f))
                // Visor
                val visor = Path().apply {
                    moveTo(p(18f, 76f))
                    lineTo(p(82f, 76f))
                    cubicTo(p(78f, 90f), p(22f, 90f), p(18f, 76f))
                    close()
                }
                drawPath(visor, blue)
            }

            EmergencyServiceKind.AMBULANCE -> {
                drawCircle(red, radius = 48f * u, center = p(50f, 50f))
                drawCross(p(50f, 50f), 56f * u, 20f * u, SurfaceWhite)
            }

            EmergencyServiceKind.HOSPITAL -> {
                // Roof block with a cross, then the main block with a window grid and door
                drawRoundRect(blue, topLeft = p(30f, 2f), size = Size(40f * u, 30f * u), cornerRadius = corner(4f))
                drawCross(p(50f, 17f), 20f * u, 7f * u, SurfaceWhite)
                drawRoundRect(blue, topLeft = p(10f, 28f), size = Size(80f * u, 70f * u), cornerRadius = corner(5f))
                listOf(38f, 58f).forEach { row ->
                    listOf(20f, 44f, 68f).forEach { col ->
                        drawRoundRect(
                            SurfaceWhite,
                            topLeft = p(col, row),
                            size = Size(12f * u, 12f * u),
                            cornerRadius = corner(1.5f)
                        )
                    }
                }
                drawRoundRect(SurfaceWhite, topLeft = p(43f, 78f), size = Size(14f * u, 20f * u), cornerRadius = corner(1.5f))
            }
        }
    }
}

/** Classical-building glyph for the "Nearest Emergency Services" heading. */
@Composable
fun InstitutionIcon(
    modifier: Modifier = Modifier,
    color: Color = InfoBlue
) {
    Canvas(modifier = modifier) {
        val u = size.minDimension / 100f
        val roof = Path().apply {
            moveTo(50f * u, 12f * u)
            lineTo(94f * u, 36f * u)
            lineTo(6f * u, 36f * u)
            close()
        }
        drawPath(roof, color)
        listOf(16f, 34f, 52f, 70f).forEach { left ->
            drawRoundRect(
                color = color,
                topLeft = Offset(left * u, 42f * u),
                size = Size(14f * u, 34f * u),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * u)
            )
        }
        drawRoundRect(
            color = color,
            topLeft = Offset(6f * u, 80f * u),
            size = Size(88f * u, 10f * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * u)
        )
    }
}

/** Two-person glyph for the "Trusted Contacts" heading. */
@Composable
fun PeopleIcon(
    modifier: Modifier = Modifier,
    color: Color = PukaarRed
) {
    Canvas(modifier = modifier) {
        val u = size.minDimension / 100f
        drawCircle(color, 15f * u, Offset(34f * u, 32f * u))
        drawCircle(color, 13f * u, Offset(70f * u, 34f * u))
        drawRoundRect(
            color = color,
            topLeft = Offset(48f * u, 52f * u),
            size = Size(44f * u, 30f * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f * u)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(8f * u, 54f * u),
            size = Size(52f * u, 32f * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f * u)
        )
    }
}

private fun Path.moveTo(point: Offset) = moveTo(point.x, point.y)
private fun Path.lineTo(point: Offset) = lineTo(point.x, point.y)
private fun Path.cubicTo(control1: Offset, control2: Offset, end: Offset) =
    cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)

private fun DrawScope.drawCross(centre: Offset, length: Float, thickness: Float, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(centre.x - thickness / 2f, centre.y - length / 2f),
        size = Size(thickness, length),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(thickness / 4f)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(centre.x - length / 2f, centre.y - thickness / 2f),
        size = Size(length, thickness),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(thickness / 4f)
    )
}

/** Map location pin, also used as the standalone location marker. */
@Composable
fun LocationPin(
    modifier: Modifier = Modifier,
    color: Color = PukaarRed
) {
    Canvas(modifier = modifier) {
        drawPin(Offset(size.width / 2f, size.height / 2f), size.minDimension, color)
    }
}

private fun DrawScope.drawPin(centre: Offset, extent: Float, color: Color) {
    val r = extent * 0.3f
    val head = Offset(centre.x, centre.y - extent * 0.15f)
    val tail = Path().apply {
        moveTo(head.x - r * 0.85f, head.y + r * 0.6f)
        lineTo(head.x + r * 0.85f, head.y + r * 0.6f)
        lineTo(head.x, head.y + r * 2.1f)
        close()
    }
    drawPath(tail, color)
    drawCircle(color, radius = r, center = head)
    drawCircle(SurfaceWhite, radius = r * 0.4f, center = head)
}

/**
 * Static preview of the sender's surroundings. A real map tile can replace this once the
 * maps SDK is wired in; the label and pin already come from the alert payload.
 */
@Composable
fun MapThumbnail(
    label: String,
    highwayLabel: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(MapLand)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Terrain shading
            drawPath(
                Path().apply {
                    moveTo(0f, h * 0.55f)
                    cubicTo(w * 0.25f, h * 0.35f, w * 0.5f, h * 0.75f, w, h * 0.45f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                },
                Color(0xFFC7E4C9)
            )

            // River
            drawPath(
                Path().apply {
                    moveTo(w * 0.62f, 0f)
                    cubicTo(w * 0.66f, h * 0.3f, w * 0.55f, h * 0.55f, w * 0.6f, h)
                },
                MapWater,
                style = Stroke(width = w * 0.035f)
            )

            // Highway
            drawPath(
                Path().apply {
                    moveTo(w * 0.78f, 0f)
                    cubicTo(w * 0.86f, h * 0.35f, w * 0.74f, h * 0.6f, w * 0.82f, h)
                },
                MapRoad,
                style = Stroke(width = w * 0.045f)
            )

            drawPin(Offset(w * 0.32f, h * 0.34f), h * 0.5f, PukaarRed)
        }

        Text(
            text = label,
            color = TextPrimary,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp)
        )

        if (highwayLabel != null) {
            Text(
                text = highwayLabel,
                color = TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MapRoad)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

/** Battery glyph used in the device-status card. */
@Composable
fun BatteryIcon(
    percent: Int,
    modifier: Modifier = Modifier,
    color: Color = CallGreen
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyTop = h * 0.12f
        val stroke = w * 0.09f
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.22f, h * 0.04f),
            size = Size(w * 0.56f, h * 0.09f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, bodyTop),
            size = Size(w * 0.76f, h * 0.84f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
            style = Stroke(width = stroke)
        )
        val inset = stroke * 1.6f
        val innerTop = bodyTop + inset
        val innerHeight = h * 0.84f - inset * 2
        val fill = innerHeight * (percent.coerceIn(0, 100) / 100f)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f + inset, innerTop + (innerHeight - fill)),
            size = Size(w * 0.76f - inset * 2, fill),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f)
        )
    }
}

/** Signal-strength bars used in the device-status card. */
@Composable
fun SignalBarsIcon(
    filledBars: Int,
    modifier: Modifier = Modifier,
    color: Color = CallGreen
) {
    Canvas(modifier = modifier) {
        val barCount = 4
        val gap = size.width * 0.08f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        repeat(barCount) { index ->
            val barHeight = size.height * (0.32f + 0.23f * index)
            drawRoundRect(
                color = if (index < filledBars) color else color.copy(alpha = 0.25f),
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth * 0.3f)
            )
        }
    }
}
