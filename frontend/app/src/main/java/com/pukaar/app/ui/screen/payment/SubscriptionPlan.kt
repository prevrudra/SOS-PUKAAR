package com.pukaar.app.ui.screen.payment

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.pukaar.app.R

/**
 * What PUKAAR sells, in the order the plans screen lists them.
 *
 * [period] is null for the one-off purchase; everything else renews yearly and
 * shows its monthly equivalent in [priceNote] so the yearly figure reads smaller.
 */
enum class SubscriptionPlan(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val price: Int,
    @StringRes val period: Int?,
    @StringRes val priceNote: Int,
    val accent: Color
) {
    EMERGENCY_CARD(
        R.string.plans_emergency_card,
        R.string.plans_emergency_card_description,
        R.string.plans_emergency_card_price,
        period = null,
        priceNote = R.string.plans_one_time_purchase,
        accent = Color(0xFF8B3DFF)
    ),
    PERSONAL(
        R.string.plans_personal,
        R.string.plans_personal_description,
        R.string.plans_personal_price,
        period = R.string.plans_per_year,
        priceNote = R.string.plans_personal_monthly,
        accent = Color(0xFF16A34A)
    ),
    GLOBAL(
        R.string.plans_global,
        R.string.plans_global_description,
        R.string.plans_global_price,
        period = R.string.plans_per_year,
        priceNote = R.string.plans_global_monthly,
        accent = Color(0xFF1D7BF2)
    ),
    PERSONAL_GROUP(
        R.string.plans_personal_group,
        R.string.plans_personal_group_description,
        R.string.plans_personal_group_price,
        period = R.string.plans_per_year,
        priceNote = R.string.plans_personal_group_monthly,
        accent = Color(0xFFD4A62A)
    ),
    GLOBAL_GROUP(
        R.string.plans_global_group,
        R.string.plans_global_group_description,
        R.string.plans_global_group_price,
        period = R.string.plans_per_year,
        priceNote = R.string.plans_global_group_monthly,
        accent = Color(0xFF1D7BF2)
    )
}
