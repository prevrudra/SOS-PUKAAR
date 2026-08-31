package com.pukaar.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.TextPrimary

/** The wordmark: "PUK" in white, "AAR" in the brand red. */
@Composable
fun PukaarWordmark(modifier: Modifier = Modifier, fontSize: Int = 24) {
    val name = stringResource(R.string.app_name).uppercase()
    val split = (name.length / 2).coerceAtMost(name.length)

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = TextPrimary)) { append(name.take(split)) }
            withStyle(SpanStyle(color = PukaarRed)) { append(name.drop(split)) }
        },
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier
    )
}

/** The shield badge. Drawn rather than shipped as an asset so it scales cleanly. */
@Composable
fun PukaarShield(modifier: Modifier = Modifier, size: Int = 34) {
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = PukaarRed,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = stringResource(R.string.app_name).take(1).uppercase(),
            color = TextPrimary,
            fontSize = (size * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = (-size * 0.05f).dp)
        )
    }
}
