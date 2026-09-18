package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.core.extension.showToast
import com.pukaar.app.ui.theme.PukaarTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The card as a lock-screen wallpaper, previewed inside a phone.
 *
 * The point of the whole feature in one screen: a stranger picks up a phone they
 * cannot unlock, and the information is already on it. The mock phone shows the
 * card under a clock so the user can see that it fits before committing their
 * wallpaper to it.
 *
 * The wallpaper is made from the very card drawn in the preview rather than a
 * second rendering of it, so the two cannot drift apart. It is captured at the
 * lock screen's own resolution, not the preview's, so text and the QR code stay
 * sharp once blown up to full width.
 */
@Composable
fun LockScreenCardScreen(
    draft: EmergencyCardDraft,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    style: CardQrStyle = CardQrStyle.WITH_DETAILS
) {
    val relationLabel = rememberRelationLabel()
    val payload = remember(draft) { emergencyCardPayload(draft, relationLabel) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardCapture = rememberGraphicsLayer()
    val targetCardWidth = remember(context) {
        lockScreenSize(context).width * LockScreenCardWidthFraction
    }
    var applying by remember { mutableStateOf(false) }

    EmergencyCardScaffold(
        onBack = onBack,
        modifier = modifier,
        footer = {
            CardPrimaryButton(
                text = stringResource(
                    if (applying) R.string.card_lock_screen_applying else R.string.card_set_lock_screen
                ),
                onClick = {
                    applying = true
                    scope.launch {
                        val result = try {
                            val card = cardCapture.toImageBitmap().asAndroidBitmap()
                            setCardAsLockScreenWallpaper(context, card)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            LockScreenResult.FAILED
                        }
                        applying = false
                        context.showToast(
                            when (result) {
                                LockScreenResult.SET -> R.string.card_lock_screen_set
                                LockScreenResult.NOT_ALLOWED -> R.string.card_lock_screen_not_allowed
                                LockScreenResult.FAILED -> R.string.card_lock_screen_failed
                            }
                        )
                    }
                },
                enabled = !applying,
                icon = Icons.Filled.PhoneAndroid,
                iconLeading = true
            )
        }
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.card_lock_screen_title),
            color = CardPalette.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.card_lock_screen_subtitle),
            color = CardPalette.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        PhoneFrame {
            LockScreenClock()
            Spacer(modifier = Modifier.height(14.dp))
            EmergencyCardFace(
                draft = draft,
                payload = payload,
                relationLabel = { contact -> relationLabel(contact) },
                compact = true,
                style = style,
                modifier = Modifier.captureInto(cardCapture, targetCardWidth)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * Draws as normal, and also records the same drawing into [layer] scaled up so it
 * comes out [targetWidth] pixels wide. Recording is only a display list; nothing
 * is rasterised until the layer is turned into a bitmap.
 */
private fun Modifier.captureInto(layer: GraphicsLayer, targetWidth: Float): Modifier =
    drawWithContent {
        drawContent()
        if (size.width <= 0f || size.height <= 0f) return@drawWithContent

        val factor = (targetWidth / size.width).coerceAtLeast(1f)
        layer.record(
            size = IntSize(
                (size.width * factor).roundToInt(),
                (size.height * factor).roundToInt()
            )
        ) {
            scale(factor, pivot = Offset.Zero) {
                this@drawWithContent.drawContent()
            }
        }
    }

/** A rounded dark slab with a bezel — enough phone to read as one. */
@Composable
private fun PhoneFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val outer = RoundedCornerShape(26.dp)
    val inner = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 44.dp)
            .background(Color(0xFF111318), outer)
            .border(1.dp, CardPalette.BorderStrong, outer)
            .padding(7.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    // A night wallpaper behind the card, so the white card reads
                    // as sitting on a lock screen rather than on the page.
                    Brush.verticalGradient(
                        listOf(LockScreenWallpaperTop, LockScreenWallpaperBottom)
                    ),
                    inner
                )
                .padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            content()
        }
    }
}

/** The time and date a lock screen shows above everything else. */
@Composable
private fun LockScreenClock(modifier: Modifier = Modifier) {
    val now = remember { Date() }
    val time = remember { SimpleDateFormat("h:mm", Locale.getDefault()).format(now) }
    val date = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = time,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = date,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LockScreenCardScreenPreview() {
    PukaarTheme {
        LockScreenCardScreen(
            draft = SampleCardDraft,
            onBack = {}
        )
    }
}
