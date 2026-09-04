package com.pukaar.app.ui.screen.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.SecondaryButton
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.component.SuccessButton
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextTertiary

/** Menu item 7. Plan status, and the two things you can do about it. */
@Composable
fun PaymentReferralScreen(
    planName: String,
    validTill: String,
    referralCode: String,
    isActive: Boolean,
    individualPrice: Int,
    familyPrice: Int,
    referralCount: Long,
    onBack: () -> Unit,
    onUpgradeIndividual: () -> Unit,
    onUpgradeFamily: () -> Unit,
    onViewHistory: () -> Unit,
    onShareReferral: () -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarScreen(
        title = stringResource(R.string.payment_title),
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SuccessButton(
                    text = stringResource(R.string.payment_individual) + " — ₹$individualPrice/yr",
                    onClick = onUpgradeIndividual
                )
                SuccessButton(
                    text = stringResource(R.string.payment_family) + " — ₹$familyPrice/yr",
                    onClick = onUpgradeFamily
                )
                SecondaryButton(
                    text = stringResource(R.string.payment_history),
                    onClick = onViewHistory
                )
            }
        }
    ) {
        SectionCard {
            Text(
                text = stringResource(R.string.payment_current_plan),
                color = TextTertiary,
                fontSize = 11.sp
            )
            Text(
                text = planName,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.payment_valid_till),
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = validTill,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (isActive) {
                    ActiveBadge()
                }
            }
        }

        ReferralCard(
            code = referralCode,
            referralCount = referralCount,
            onShare = onShareReferral
        )
    }
}

@Composable
private fun ReferralCard(
    code: String,
    referralCount: Long,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.referral_title),
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.payment_referrals_count, referralCount),
            color = TextTertiary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = stringResource(R.string.referral_description),
            color = TextTertiary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.referral_code_label),
            color = TextTertiary,
            fontSize = 11.sp
        )
        Text(
            text = code,
            color = SuccessGreen,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        SecondaryButton(
            text = stringResource(R.string.referral_share),
            onClick = onShare
        )
    }
}

@Composable
private fun ActiveBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SuccessGreen, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.payment_active).uppercase(),
            color = androidx.compose.ui.graphics.Color.Black,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun PaymentReferralScreenPreview() {
    PukaarTheme {
        PaymentReferralScreen(
            planName = stringResource(R.string.payment_premium),
            validTill = stringResource(R.string.payment_valid_date),
            referralCode = stringResource(R.string.referral_code),
            isActive = true,
            individualPrice = 499,
            familyPrice = 699,
            referralCount = 0,
            onBack = {},
            onUpgradeIndividual = {},
            onUpgradeFamily = {},
            onViewHistory = {},
            onShareReferral = {}
        )
    }
}
