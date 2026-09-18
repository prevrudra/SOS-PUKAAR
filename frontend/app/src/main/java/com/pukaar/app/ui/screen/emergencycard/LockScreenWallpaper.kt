package com.pukaar.app.ui.screen.emergencycard

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The night sky behind the card, top to bottom. Shared with the phone preview so
 * what the user sees before tapping is what ends up on their lock screen.
 */
internal val LockScreenWallpaperTop = Color(0xFF1B2333)
internal val LockScreenWallpaperBottom = Color(0xFF2A1A22)

/** How much of the screen's width the card spans. */
internal const val LockScreenCardWidthFraction = 0.86f

/**
 * The vertical band the card is kept inside. Above it sits the clock and
 * notifications, below it the unlock hint and shortcuts — both drawn by the
 * system over the wallpaper, so a card there would be half covered.
 */
private const val CardBandTop = 0.28f
private const val CardBandBottom = 0.86f

enum class LockScreenResult { SET, NOT_ALLOWED, FAILED }

/**
 * The phone's full screen in pixels, portrait way up — including the status and
 * navigation bars, since the lock screen wallpaper runs under both.
 */
fun lockScreenSize(context: Context): IntSize {
    val windowManager = context.getSystemService(WindowManager::class.java)
    val (width, height) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.maximumWindowMetrics.bounds
        bounds.width() to bounds.height()
    } else {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        metrics.widthPixels to metrics.heightPixels
    }
    return IntSize(minOf(width, height), maxOf(width, height))
}

/**
 * Puts [card] on the lock screen, leaving the home screen wallpaper alone.
 *
 * Drawing and handing a full-screen bitmap to the system takes long enough to
 * drop frames, so it runs off the main thread. Some managed devices forbid
 * changing the wallpaper at all, which is told apart from a plain failure so the
 * user is not asked to retry something that can never work.
 */
suspend fun setCardAsLockScreenWallpaper(context: Context, card: Bitmap): LockScreenResult =
    withContext(Dispatchers.IO) {
        val manager = WallpaperManager.getInstance(context)
        if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) {
            return@withContext LockScreenResult.NOT_ALLOWED
        }

        try {
            val wallpaper = composeWallpaper(card, lockScreenSize(context))
            val id = manager.setBitmap(wallpaper, null, true, WallpaperManager.FLAG_LOCK)
            wallpaper.recycle()
            if (id != 0) LockScreenResult.SET else LockScreenResult.FAILED
        } catch (error: Exception) {
            LockScreenResult.FAILED
        }
    }

/** The gradient, with the card scaled to fit and centred in the free band. */
private fun composeWallpaper(card: Bitmap, screen: IntSize): Bitmap {
    // A capture from the GPU can come back as a hardware bitmap, which a software
    // canvas refuses to draw.
    val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        card.config == Bitmap.Config.HARDWARE
    ) {
        card.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        card
    }

    val width = screen.width.toFloat()
    val height = screen.height.toFloat()
    val wallpaper = Bitmap.createBitmap(screen.width, screen.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(wallpaper)

    canvas.drawRect(
        0f, 0f, width, height,
        Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height,
                LockScreenWallpaperTop.toArgb(), LockScreenWallpaperBottom.toArgb(),
                Shader.TileMode.CLAMP
            )
        }
    )

    val bandTop = height * CardBandTop
    val bandHeight = height * (CardBandBottom - CardBandTop)
    val scale = minOf(
        width * LockScreenCardWidthFraction / source.width,
        bandHeight / source.height
    )
    val cardWidth = source.width * scale
    val cardHeight = source.height * scale
    val left = (width - cardWidth) / 2f
    val top = bandTop + (bandHeight - cardHeight) / 2f

    canvas.drawBitmap(
        source,
        null,
        RectF(left, top, left + cardWidth, top + cardHeight),
        Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    )

    if (source !== card) source.recycle()
    return wallpaper
}
