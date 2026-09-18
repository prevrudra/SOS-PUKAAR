package com.pukaar.app.ui.screen.tripshield

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.theme.AccentAmber
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary

/** Where the label flips from white to red: TRIP | SHIELD. */
private const val WORDMARK_SPLIT = 4

/**
 * The TripShield masthead: the wordmark with a plane banking away from a dashed
 * flight path, the strapline, and the audience line ruled off on both sides.
 */
@Composable
fun TripShieldHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlightPath()
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = wordmark(stringResource(R.string.tripshield_wordmark)),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.tripshield_strapline),
            color = TextPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))
        RuledCaption(text = stringResource(R.string.menu_tripshield_badge))
    }
}

/** TRIP in white, SHIELD in the brand red. */
private fun wordmark(name: String) = buildAnnotatedString {
    val split = WORDMARK_SPLIT.coerceIn(0, name.length)
    withStyle(SpanStyle(color = TextPrimary)) { append(name.take(split)) }
    withStyle(SpanStyle(color = PukaarRed)) { append(name.drop(split)) }
}

/**
 * The plane and the dashed arc trailing it.
 *
 * Drawn rather than shipped as an asset so it stays crisp at any density and
 * picks up the brand red from the theme.
 */
@Composable
private fun FlightPath(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 56.dp, height = 52.dp)) {
        Canvas(modifier = Modifier.size(width = 56.dp, height = 52.dp)) {
            // A little under half a circle, opening to the right, so the trail
            // sweeps down and back from beneath the plane.
            val arc = Path().apply {
                addArc(
                    Rect(
                        offset = Offset(size.width * 0.02f, size.height * 0.30f),
                        size = Size(size.width * 0.52f, size.height * 0.62f)
                    ),
                    -60f,
                    260f
                )
            }
            drawPath(
                path = arc,
                color = PukaarRed,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                    )
                )
            )
        }

        Icon(
            imageVector = Icons.Filled.Flight,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(30.dp)
                .rotate(45f)
        )
    }
}

/** A line of text with a rule running out to either side. */
@Composable
private fun RuledCaption(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Rule()
        Text(
            text = text,
            color = AccentAmber,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Rule()
    }
}

@Composable
private fun Rule() {
    Canvas(modifier = Modifier.size(width = 26.dp, height = 1.dp)) {
        drawLine(
            color = AccentAmber,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true, widthDp = 360)
@Composable
private fun TripShieldHeaderPreview() {
    PukaarTheme { TripShieldHeader() }
}
