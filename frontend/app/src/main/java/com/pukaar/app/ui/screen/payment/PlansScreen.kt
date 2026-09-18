package com.pukaar.app.ui.screen.payment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.CircularBackButton
import com.pukaar.app.ui.component.PukaarWordmark
import com.pukaar.app.ui.screen.emergencycard.QrCode
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarRedBright
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(18.dp)
private val DescriptionColor = Color(0xFFC4C4C4)

/** The people icon on each group plan. */
private val GroupGlobalIcon = Color(0xFF4DA6FF)
private val GroupPersonalIcon = Color(0xFFF5C542)

/**
 * Menu item 7: every plan on one screen, each card opening its purchase.
 *
 * The Emergency Card leads because it is the cheapest way in; the group plans
 * trail because they only make sense once the single plan beside them is read.
 */
@Composable
fun PlansScreen(
    onBack: () -> Unit,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(top = 10.dp)) {
                CircularBackButton(onClick = onBack)
            }
            BrandLockup(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.plans_title),
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.plans_subtitle),
                color = DescriptionColor,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SubscriptionPlan.entries.forEach { plan ->
                    PlanCard(plan = plan, onClick = { onPlanSelected(plan) })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            TrustRow()

            Spacer(modifier = Modifier.height(22.dp))
            BePrepared()
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/** The app icon beside the wordmark, with the promise set small underneath. */
@Composable
private fun BrandLockup(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            // A raster copy of the launcher icon: R.mipmap.ic_launcher is an
            // adaptive-icon XML on API 26+, which painterResource cannot draw.
            painter = painterResource(R.drawable.pukaar_app_icon),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            PukaarWordmark(fontSize = 28)
            Text(
                text = stringResource(R.string.plans_always_with_you).uppercase(),
                color = TextPrimary,
                fontSize = 9.sp,
                letterSpacing = 2.5.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = plan.accent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color(0xFF080808))
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.06f))
                )
            )
            .border(1.5.dp, accent, CardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(92.dp), contentAlignment = Alignment.Center) {
            PlanVisual(plan)
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .width(1.dp)
                .height(76.dp)
                .background(Color.White.copy(alpha = 0.18f))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(plan.title),
                color = TextPrimary,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(plan.description),
                color = DescriptionColor,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 3.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            // Wraps the pill under the price when a narrow phone can't fit both.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PriceText(plan, modifier = Modifier.align(Alignment.CenterVertically))
                PricePill(plan, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun PriceText(plan: SubscriptionPlan, modifier: Modifier = Modifier) {
    val price = stringResource(plan.price)
    val period = plan.period?.let { stringResource(it) }

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold)) {
                append(price)
            }
            if (period != null) {
                withStyle(SpanStyle(fontSize = 14.sp, color = DescriptionColor)) {
                    append("  ")
                    append(period)
                }
            }
        },
        color = TextPrimary,
        modifier = modifier.padding(end = 8.dp)
    )
}

@Composable
private fun PricePill(plan: SubscriptionPlan, modifier: Modifier = Modifier) {
    // The one-off purchase gets a quieter tint: it is a label, not a saving.
    val fill = when (plan) {
        SubscriptionPlan.EMERGENCY_CARD -> plan.accent.copy(alpha = 0.45f)
        SubscriptionPlan.PERSONAL_GROUP -> Color(0xFFB8891A)
        else -> plan.accent
    }

    Box(
        modifier = modifier
            .background(fill, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(plan.priceNote),
            color = TextPrimary,
            fontSize = 12.5.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlanVisual(plan: SubscriptionPlan) {
    when (plan) {
        SubscriptionPlan.EMERGENCY_CARD -> MiniEmergencyCard(glow = plan.accent)
        SubscriptionPlan.PERSONAL -> IconDisc(Icons.Outlined.Person, plan.accent)
        SubscriptionPlan.GLOBAL -> IconDisc(Icons.Outlined.Language, plan.accent)
        SubscriptionPlan.PERSONAL_GROUP -> GroupBadge(GroupPersonalIcon, plan.accent)
        SubscriptionPlan.GLOBAL_GROUP -> GroupBadge(GroupGlobalIcon, GroupGlobalIcon)
    }
}

@Composable
private fun IconDisc(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(40.dp)
        )
    }
}

/** Three people over the "Pay for 2, Get 1 Free" tag, the tag tucked up under them. */
@Composable
private fun GroupBadge(iconColor: Color, tagColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(58.dp)
        )
        Box(
            modifier = Modifier
                .offset(y = (-8).dp)
                .background(Black, RoundedCornerShape(10.dp))
                .border(1.5.dp, tagColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.plans_pay_2_get_1),
                color = tagColor,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A small, tilted Emergency Card lit from behind.
 *
 * The QR only has to look like one at this size, so it encodes the app name
 * rather than anybody's details.
 */
@Composable
private fun MiniEmergencyCard(glow: Color) {
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .rotate(-10f)
            .shadow(14.dp, shape, ambientColor = glow, spotColor = glow)
            .size(width = 88.dp, height = 56.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFF1C1C1C), Color(0xFF050505))),
                shape
            )
            .border(0.5.dp, Outline, shape)
            .padding(6.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            PukaarWordmark(fontSize = 10)
            Text(
                text = stringResource(R.string.plans_emergency_card).uppercase(),
                color = TextPrimary,
                fontSize = 5.sp,
                letterSpacing = 0.8.sp
            )
        }
        QrCode(
            payload = stringResource(R.string.app_name),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(22.dp)
        )
    }
}

@Composable
private fun TrustRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrustItem(
            icon = Icons.Outlined.VerifiedUser,
            text = stringResource(R.string.plans_trust_families),
            modifier = Modifier.weight(1f)
        )
        TrustDivider()
        TrustItem(
            icon = Icons.Outlined.Groups,
            text = stringResource(R.string.plans_trust_real_life),
            modifier = Modifier.weight(1f)
        )
        TrustDivider()
        TrustItem(
            icon = Icons.Outlined.Language,
            text = stringResource(R.string.plans_trust_borders),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TrustItem(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = DescriptionColor,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun TrustDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(TextSecondary.copy(alpha = 0.5f))
    )
}

/** The closing line, ruled in red on both sides, and the call underlined by hand. */
@Composable
private fun BePrepared(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RedRule(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.plans_every_second_matters),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            RedRule(modifier = Modifier.weight(1f))
        }

        Text(
            text = stringResource(R.string.plans_be_prepared),
            color = PukaarRedBright,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .padding(top = 2.dp, bottom = 10.dp)
                .drawBehind {
                    // A brush stroke rather than a ruler line: it dips in the
                    // middle and thins out to the right.
                    val y = size.height + 4.dp.toPx()
                    val swoosh = Path().apply {
                        moveTo(size.width * 0.05f, y + 3.dp.toPx())
                        cubicTo(
                            size.width * 0.35f, y - 2.dp.toPx(),
                            size.width * 0.7f, y - 4.dp.toPx(),
                            size.width * 0.95f, y - 2.dp.toPx()
                        )
                    }
                    drawPath(
                        path = swoosh,
                        color = PukaarRedBright,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
        )
    }
}

@Composable
private fun RedRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(2.dp)
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, PukaarRedBright, Color.Transparent)
                    ),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height
                )
            }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, heightDp = 1500)
@Composable
private fun PlansScreenPreview() {
    PukaarTheme {
        PlansScreen(onBack = {}, onPlanSelected = {})
    }
}
