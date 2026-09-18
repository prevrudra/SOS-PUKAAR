package com.pukaar.app.ui.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pukaar.app.R

/** WhatsApp, and WhatsApp Business, in the order they are worth trying. */
private val WhatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

const val HIGH_ALERT_PACKAGE_ID = "com.pukaar.highalert"
const val HIGH_ALERT_PLAY_URL =
    "https://play.google.com/store/apps/details?id=$HIGH_ALERT_PACKAGE_ID"

/**
 * The invite, addressed to WhatsApp first.
 *
 * WhatsApp is where this actually gets sent, so it is tried by name rather than
 * being buried in a share sheet among a dozen other apps. If it is not installed
 * — or is installed but has no activity for a plain-text send — the generic
 * chooser takes over, so the tile always does something.
 *
 * Both are handled, since WhatsApp Business is a separate package and plenty of
 * people have only that one.
 */
fun shareInvite(context: Context, message: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    for (whatsApp in WhatsAppPackages) {
        try {
            context.startActivity(Intent(send).setPackage(whatsApp))
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next one, then fall through to the chooser.
        }
    }

    context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * Opens WhatsApp chat to [phoneE164] with the High Alert download message prefilled
 * when possible; otherwise falls back to a generic WhatsApp / share sheet send.
 */
fun shareHighAlertApp(context: Context, phoneE164: String? = null) {
    val message = context.getString(R.string.high_alert_share_message)
    val chooserTitle = context.getString(R.string.high_alert_share_chooser)
    val digits = phoneE164?.filter { it.isDigit() }.orEmpty()
    if (digits.length >= 10) {
        val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
        val wa = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        for (pkg in WhatsAppPackages) {
            try {
                context.startActivity(Intent(wa).setPackage(pkg))
                return
            } catch (_: ActivityNotFoundException) {
            }
        }
        runCatching { context.startActivity(wa) }.getOrElse {
            shareInvite(context, message, chooserTitle)
        }
        return
    }
    shareInvite(context, message, chooserTitle)
}

/**
 * The invite ready to fire, so the caller needs no Context of its own.
 *
 * The store link is built from the running package rather than written down: a
 * hardcoded one survives a rename of the app id and quietly points at nothing.
 */
@Composable
fun rememberInviteAction(): () -> Unit {
    val context = LocalContext.current
    val message = stringResource(R.string.menu_invite_message, context.packageName)
    val chooserTitle = stringResource(R.string.menu_invite_chooser)

    return remember(context, message, chooserTitle) {
        { shareInvite(context, message, chooserTitle) }
    }
}
