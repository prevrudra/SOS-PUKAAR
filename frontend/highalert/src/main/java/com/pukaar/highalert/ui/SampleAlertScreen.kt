package com.pukaar.highalert.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pukaar.highalert.data.AlertLocation
import com.pukaar.highalert.data.AlertRepository
import com.pukaar.highalert.data.DeliveryStatus
import com.pukaar.highalert.data.DeviceStatus
import com.pukaar.highalert.data.EmergencyService
import com.pukaar.highalert.data.HelpContact
import com.pukaar.highalert.data.SosAlert
import com.pukaar.highalert.data.TrustedContact
import com.pukaar.highalert.ui.components.AlertBellIcon
import com.pukaar.highalert.ui.components.BatteryIcon
import com.pukaar.highalert.ui.components.EmergencyServiceGlyph
import com.pukaar.highalert.ui.components.InstitutionIcon
import com.pukaar.highalert.ui.components.LocationPin
import com.pukaar.highalert.ui.components.MapThumbnail
import com.pukaar.highalert.ui.components.PeopleIcon
import com.pukaar.highalert.ui.components.SignalBarsIcon
import com.pukaar.highalert.ui.theme.AlertBadgeRed
import com.pukaar.highalert.ui.theme.AlertBannerPink
import com.pukaar.highalert.ui.theme.AlertCallGreen
import com.pukaar.highalert.ui.theme.AlertDivider
import com.pukaar.highalert.ui.theme.AlertDividerGreen
import com.pukaar.highalert.ui.theme.AlertDividerLight
import com.pukaar.highalert.ui.theme.AlertEmergencyRed
import com.pukaar.highalert.ui.theme.AlertGrey
import com.pukaar.highalert.ui.theme.AlertHeadlineRed
import com.pukaar.highalert.ui.theme.AlertHelpPhoneGreen
import com.pukaar.highalert.ui.theme.AlertHelpSurface
import com.pukaar.highalert.ui.theme.AlertInk
import com.pukaar.highalert.ui.theme.AlertInkSoft
import com.pukaar.highalert.ui.theme.AlertInstitutionGrey
import com.pukaar.highalert.ui.theme.AlertLinkBlue
import com.pukaar.highalert.ui.theme.AlertNetworkGreen
import com.pukaar.highalert.ui.theme.AlertNeutralSurface
import com.pukaar.highalert.ui.theme.AlertPeopleBlue
import com.pukaar.highalert.ui.theme.AlertSenderSurface
import com.pukaar.highalert.ui.theme.AlertServiceBlue
import com.pukaar.highalert.ui.theme.AlertServiceRed
import com.pukaar.highalert.ui.theme.AlertServicesHeader
import com.pukaar.highalert.ui.theme.AlertServicesSurface
import com.pukaar.highalert.ui.theme.AlertStatusSurface
import com.pukaar.highalert.ui.theme.AlertSubtitleGrey
import com.pukaar.highalert.ui.theme.AlertTitleRed
import com.pukaar.highalert.ui.theme.AlertTrustedBlue
import com.pukaar.highalert.ui.theme.AlertTrustedSurface
import com.pukaar.highalert.ui.theme.PukaarAlertTheme
import com.pukaar.highalert.ui.theme.PukaarRed
import com.pukaar.highalert.ui.theme.PukaarRedSoft
import com.pukaar.highalert.ui.theme.StatusDeliveredSurface
import com.pukaar.highalert.ui.theme.StatusDeliveredText
import com.pukaar.highalert.ui.theme.StatusPendingSurface
import com.pukaar.highalert.ui.theme.StatusPendingText
import com.pukaar.highalert.ui.theme.StatusReadSurface
import com.pukaar.highalert.ui.theme.StatusReadText
import com.pukaar.highalert.ui.theme.SurfaceWhite

@Composable
fun SampleAlertScreen(
    alert: SosAlert,
    onBack: (() -> Unit)? = null,
    title: String = "PUKAAR SOS Alert",
    onBeforeAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dial: (String) -> Unit = remember(context, onBeforeAction) {
        {
            onBeforeAction?.invoke()
            DeviceActions.dial(context, it)
        }
    }

    Column(
        modifier = modifier
            .background(SurfaceWhite)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        if (onBack != null) {
            ScreenHeader(title = title, onBack = onBack)
        } else {
            Text(
                text = title,
                style = TextStyle(
                    color = Color(0xFF111827),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AlertBanner(alert)
            SenderCard(alert, onCall = { dial(alert.senderPhone) })
            if (alert.trustedContacts.isNotEmpty()) {
                TrustedContactsCard(alert.trustedContacts, onCall = dial)
            }
            if (alert.helpContacts.isNotEmpty()) {
                HelpNumbersCard(alert.senderName, alert.helpContacts, onCall = dial)
            }
            LocationCard(
                senderName = alert.senderName,
                location = alert.location,
                onOpenMap = {
                    onBeforeAction?.invoke()
                    DeviceActions.openMap(
                        context = context,
                        latitude = alert.location.latitude,
                        longitude = alert.location.longitude,
                        label = alert.senderName
                    )
                }
            )
            DeviceStatusRow(alert.deviceStatus)
            EmergencyServicesCard(
                senderName = alert.senderName,
                services = alert.emergencyServices,
                onCall = dial
            )
            Spacer(Modifier.height(4.dp))
            EmergencyCallButton(onClick = { dial(AlertRepository.EMERGENCY_NUMBER) })
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center
        ) {
            Chevron(
                pointsBack = true,
                color = AlertInk,
                modifier = Modifier.size(width = 9.dp, height = 16.dp)
            )
        }
        Text(text = title, style = HeaderTitle)
    }
}

@Composable
private fun AlertBanner(alert: SosAlert) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AlertBannerPink)
            .padding(start = 9.dp, end = 8.dp, top = 6.dp, bottom = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            AlertBellIcon(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(width = 78.dp, height = 46.dp)
            )
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        FitText(text = "PUKAAR ALERT", style = BannerTitle)
                        FitText(text = "${alert.dateLabel}  |  ${alert.timeLabel}", style = BannerDate)
                    }
                    Spacer(Modifier.width(6.dp))
                    SosBadge(label = alert.type.label, modifier = Modifier.padding(top = 3.dp))
                }
                FitText(text = alert.headline, style = BannerHeadline)
            }
        }
        FitText(
            text = alert.message,
            style = BannerMessage,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp)
        )
    }
}

@Composable
private fun SosBadge(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .widthIn(min = 56.dp)
            .heightIn(min = 24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AlertBadgeRed)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = SosLabel, maxLines = 1)
    }
}

@Composable
private fun SenderCard(alert: SosAlert, onCall: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AlertSenderSurface)
            .padding(start = 7.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = alert.senderName, photoRes = alert.senderPhotoRes, size = 57.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            FitText(text = alert.senderName, style = SenderName)
            FitText(text = alert.senderPhone, style = SenderPhone)
            FitText(text = alert.senderSubtitle, style = SenderSubtitle)
        }
        Spacer(Modifier.width(6.dp))
        CallButton(
            label = "Call ${alert.senderName}",
            onClick = onCall,
            labelStyle = SenderCallLabel,
            height = 32.dp,
            iconSize = 19.dp,
            iconGap = 10.dp,
            horizontalPadding = 14.dp
        )
    }
}

@Composable
private fun TrustedContactsCard(contacts: List<TrustedContact>, onCall: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AlertTrustedSurface)
            .padding(top = 6.dp, bottom = 5.dp)
    ) {
        SectionHeader(
            icon = { PeopleIcon(modifier = Modifier.size(26.dp), color = AlertPeopleBlue) },
            title = "Trusted Contacts",
            titleStyle = TrustedTitle,
            subtitle = "Trusted contacts are contacts who have received this message and they can " +
                "coordinate with each other."
        )
        Spacer(Modifier.height(6.dp))
        ContactColumns(
            contacts = contacts.map { ContactSummary(it.name, it.relation, it.phone, it.place, it.photoRes) },
            relationOnNameLine = true,
            avatarGap = 7.dp,
            dividerColor = AlertDivider,
            onCall = onCall,
            beforeCall = { index ->
                DeliveryChip(status = contacts[index].status, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(3.dp))
            }
        )
    }
}

@Composable
private fun HelpNumbersCard(
    senderName: String,
    contacts: List<HelpContact>,
    onCall: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AlertHelpSurface)
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        SectionHeader(
            icon = {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = AlertHelpPhoneGreen,
                    modifier = Modifier.size(26.dp)
                )
            },
            title = "Pre-saved Help Numbers",
            titleStyle = HelpTitle,
            subtitle = "People saved by $senderName who may be contacted directly for additional " +
                "help when needed."
        )
        Spacer(Modifier.height(9.dp))
        ContactColumns(
            contacts = contacts.map { ContactSummary(it.name, it.relation, it.phone, it.place, it.photoRes) },
            relationOnNameLine = false,
            avatarGap = 8.dp,
            dividerColor = AlertDividerGreen,
            onCall = onCall,
            beforeCall = { Spacer(Modifier.height(4.dp)) }
        )
    }
}

@Composable
private fun SectionHeader(
    icon: @Composable () -> Unit,
    title: String,
    titleStyle: TextStyle,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 7.dp, end = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.width(34.dp)) { icon() }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = titleStyle)
            Text(
                text = subtitle,
                style = SectionSubtitle,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private data class ContactSummary(
    val name: String,
    val relation: String,
    val phone: String,
    val place: String,
    @DrawableRes val photoRes: Int?
)

/**
 * One column per contact, split by thin dividers, with every Call button on a shared
 * baseline. Text in all columns shares one scale, shrunk just enough that numbers and
 * places stay on a single line on narrow screens or at large font scales.
 */
@Composable
private fun ContactColumns(
    contacts: List<ContactSummary>,
    relationOnNameLine: Boolean,
    avatarGap: Dp,
    dividerColor: Color,
    onCall: (String) -> Unit,
    beforeCall: @Composable ColumnScope.(index: Int) -> Unit
) {
    // Measured outside the intrinsic-height row: subcomposition cannot answer intrinsics.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val textWidth = columnWidth(maxWidth, contacts.size) -
            ContactAvatarStart - ContactAvatarSize - avatarGap - ColumnEndPadding
        val lines = contacts.flatMap { contact ->
            val nameLines = if (relationOnNameLine) {
                listOf(FitLine("${contact.name} (${contact.relation})", ContactName))
            } else {
                listOf(FitLine(contact.name, ContactName), FitLine("(${contact.relation})", ContactDetail))
            }
            nameLines + FitLine(contact.phone, ContactDetail) + FitLine(contact.place, ContactDetail)
        }
        val scale = rememberFitScale(lines, textWidth)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            contacts.forEachIndexed { index, contact ->
                if (index > 0) ColumnDivider(dividerColor)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = ContactAvatarStart, end = ColumnEndPadding),
                        verticalAlignment = Alignment.Top
                    ) {
                        Avatar(name = contact.name, photoRes = contact.photoRes, size = ContactAvatarSize)
                        Spacer(Modifier.width(avatarGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildAnnotatedString {
                                    append(contact.name)
                                    withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = AlertInkSoft)) {
                                        append(" (${contact.relation})")
                                    }
                                },
                                style = ContactName.scaled(scale)
                            )
                            Text(text = contact.phone, style = ContactDetail.scaled(scale))
                            Text(text = contact.place, style = ContactDetail.scaled(scale))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    beforeCall(index)
                    CallButton(
                        label = "Call",
                        onClick = { onCall(contact.phone) },
                        labelStyle = SmallCallLabel,
                        height = 24.dp,
                        iconSize = 15.dp,
                        modifier = Modifier.fillMaxWidth(SmallCallWidthFraction)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryChip(status: DeliveryStatus, modifier: Modifier = Modifier) {
    val (textColor, surface) = when (status) {
        DeliveryStatus.READ -> StatusReadText to StatusReadSurface
        DeliveryStatus.DELIVERED -> StatusDeliveredText to StatusDeliveredSurface
        DeliveryStatus.PENDING -> StatusPendingText to StatusPendingSurface
    }
    Row(
        modifier = modifier
            .widthIn(min = 66.dp)
            .heightIn(min = 17.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(surface)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = status.label, style = ChipLabel.copy(color = textColor), maxLines = 1)
        Spacer(Modifier.width(5.dp))
        if (status == DeliveryStatus.PENDING) {
            Text(text = "•••", style = ChipLabel.copy(color = textColor, fontWeight = FontWeight.Bold))
        } else {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun LocationCard(senderName: String, location: AlertLocation, onOpenMap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AlertNeutralSurface)
            .padding(start = 3.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            LocationPin(modifier = Modifier.size(32.dp), color = AlertServiceRed)
            Spacer(Modifier.width(9.dp))
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val title = "$senderName's Location"
                val scale = rememberFitScale(
                    listOf(FitLine(title, LocationTitle)) +
                        location.addressLines.map { FitLine(it, LocationAddress) },
                    maxWidth
                )
                Column {
                    Text(text = title, style = LocationTitle.scaled(scale))
                    Spacer(Modifier.height(3.dp))
                    location.addressLines.forEach { line ->
                        Text(text = line, style = LocationAddress.scaled(scale))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(role = Role.Button, onClick = onOpenMap)
                    ) {
                        Text(text = "View on Map", style = ViewMapLabel.scaled(scale))
                        Spacer(Modifier.width(7.dp))
                        Chevron(
                            pointsBack = false,
                            color = AlertLinkBlue,
                            modifier = Modifier.size(width = 6.dp, height = 11.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        val mapModifier = Modifier
            .weight(1f)
            .height(78.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClickLabel = "Open map", onClick = onOpenMap)
        if (location.mapPreviewRes != null) {
            Image(
                painter = painterResource(location.mapPreviewRes),
                contentDescription = "Map of ${location.mapLabel.replace('\n', ' ')}",
                contentScale = ContentScale.Crop,
                modifier = mapModifier
            )
        } else {
            MapThumbnail(
                label = location.mapLabel,
                highwayLabel = location.highwayLabel,
                modifier = mapModifier
            )
        }
    }
}

@Composable
private fun DeviceStatusRow(status: DeviceStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusTile(
            modifier = Modifier.weight(1f),
            iconWidth = 20.dp,
            iconGap = 15.dp,
            icon = {
                BatteryIcon(
                    percent = status.batteryPercent,
                    color = AlertCallGreen,
                    modifier = Modifier.size(width = 20.dp, height = 28.dp)
                )
            },
            title = "Battery",
            value = "${status.batteryPercent}%",
            valueColor = AlertInk
        )
        StatusTile(
            modifier = Modifier.weight(1f),
            iconWidth = 26.dp,
            iconGap = 17.dp,
            icon = {
                SignalBarsIcon(
                    filledBars = 4,
                    color = AlertCallGreen,
                    modifier = Modifier.size(width = 26.dp, height = 22.dp)
                )
            },
            title = "Mobile Network",
            value = "${status.networkQuality} (${status.networkType})",
            valueColor = AlertNetworkGreen
        )
    }
}

@Composable
private fun StatusTile(
    modifier: Modifier = Modifier,
    iconWidth: Dp,
    iconGap: Dp,
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AlertStatusSurface)
            .heightIn(min = 43.dp)
            .padding(start = 11.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(iconWidth), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(iconGap))
        Column(modifier = Modifier.weight(1f)) {
            FitText(text = title, style = StatusLabel)
            FitText(text = value, style = StatusValue.copy(color = valueColor))
        }
    }
}

@Composable
private fun EmergencyServicesCard(
    senderName: String,
    services: List<EmergencyService>,
    onCall: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AlertServicesSurface)
            .padding(bottom = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AlertServicesHeader)
                .padding(start = 9.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InstitutionIcon(modifier = Modifier.size(22.dp), color = AlertInstitutionGrey)
            Spacer(Modifier.width(12.dp))
            FitText(
                text = buildAnnotatedString {
                    append("Nearest Emergency Services")
                    withStyle(SpanStyle(fontSize = 0.82.em, fontWeight = FontWeight.Normal, color = AlertInkSoft)) {
                        append(" (to $senderName's location)")
                    }
                },
                style = ServicesTitle,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))
        ServiceColumns(services, onCall)
        Text(
            text = "(Numbers are subject to availability. If not reachable, please contact local authorities.)",
            style = Disclaimer,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp)
        )
    }
}

@Composable
private fun ServiceColumns(services: List<EmergencyService>, onCall: (String) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val textWidth = columnWidth(maxWidth, services.size) -
            ServiceGlyphStart - ServiceGlyphSize - ServiceGlyphGap - ColumnEndPadding
        val lines = services.flatMap { service ->
            listOf(FitLine(service.title, ServiceTitle)) +
                service.detailLines.map { FitLine(it, ServiceDetail) } +
                FitLine(service.phone, ServicePhone)
        }
        val scale = rememberFitScale(lines, textWidth)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            services.forEachIndexed { index, service ->
                if (index > 0) ColumnDivider(AlertDividerLight)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = ServiceGlyphStart, end = ColumnEndPadding),
                        verticalAlignment = Alignment.Top
                    ) {
                        EmergencyServiceGlyph(
                            kind = service.kind,
                            blue = AlertServiceBlue,
                            red = AlertServiceRed,
                            modifier = Modifier.size(ServiceGlyphSize)
                        )
                        Spacer(Modifier.width(ServiceGlyphGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = service.title, style = ServiceTitle.scaled(scale))
                            Spacer(Modifier.height(2.dp))
                            service.detailLines.forEach { line ->
                                Text(text = line, style = ServiceDetail.scaled(scale))
                            }
                            Text(text = service.phoneLabel, style = ServicePhone.scaled(scale))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(6.dp))
                    CallButton(
                        label = "Call",
                        onClick = { onCall(service.phone) },
                        labelStyle = SmallCallLabel,
                        height = 24.dp,
                        iconSize = 15.dp,
                        modifier = Modifier.fillMaxWidth(SmallCallWidthFraction)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyCallButton(onClick: () -> Unit) {
    val label = "Emergency Call ${AlertRepository.EMERGENCY_NUMBER} (${AlertRepository.EMERGENCY_REGION})"
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(AlertEmergencyRed)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val sideSpace = 36.dp
        val iconSize = 28.dp
        val iconGap = 12.dp
        val scale = rememberFitScale(
            listOf(FitLine(label, EmergencyLabel)),
            maxWidth - sideSpace * 2 - iconSize - iconGap
        )
        Row(
            modifier = Modifier.padding(horizontal = sideSpace, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = null,
                tint = SurfaceWhite,
                modifier = Modifier.size(iconSize)
            )
            Spacer(Modifier.width(iconGap))
            Text(text = label, style = EmergencyLabel.scaled(scale), textAlign = TextAlign.Center)
        }
        Chevron(
            pointsBack = false,
            color = SurfaceWhite,
            strokeWidth = 2.5.dp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(width = 8.dp, height = 15.dp)
        )
    }
}

@Composable
private fun CallButton(
    label: String,
    onClick: () -> Unit,
    labelStyle: TextStyle,
    height: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    iconGap: Dp = 8.dp,
    horizontalPadding: Dp = 6.dp
) {
    Row(
        // A minimum rather than a fixed height: at a large system font scale the label
        // needs more room than the design height and would otherwise be clipped.
        modifier = modifier
            .heightIn(min = height)
            .clip(ButtonShape)
            .background(AlertCallGreen)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = null,
            tint = SurfaceWhite,
            modifier = Modifier.size(iconSize)
        )
        Spacer(Modifier.width(iconGap))
        Text(text = label, style = labelStyle, maxLines = 1)
    }
}

@Composable
private fun Avatar(name: String, @DrawableRes photoRes: Int?, size: Dp) {
    val shapeModifier = Modifier
        .size(size)
        .clip(CircleShape)
    if (photoRes != null) {
        Image(
            painter = painterResource(photoRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = shapeModifier
        )
    } else {
        Box(
            modifier = shapeModifier.background(PukaarRedSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                color = PukaarRed
            )
        }
    }
}

/** Thin stroked chevron; "back" points left in LTR and right in RTL. */
@Composable
private fun Chevron(
    pointsBack: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp
) {
    val pointsLeft = pointsBack != (LocalLayoutDirection.current == LayoutDirection.Rtl)
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2f
        val tipX = if (pointsLeft) inset else size.width - inset
        val tailX = if (pointsLeft) size.width - inset else inset
        val path = Path().apply {
            moveTo(tailX, inset)
            lineTo(tipX, size.height / 2f)
            lineTo(tailX, size.height - inset)
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private val DividerThickness = 1.dp

@Composable
private fun ColumnDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(DividerThickness)
            .fillMaxHeight()
            .background(color)
    )
}

private fun columnWidth(totalWidth: Dp, count: Int): Dp =
    (totalWidth - DividerThickness * (count - 1).coerceAtLeast(0)) / count.coerceAtLeast(1)

/**
 * Text that keeps to one line by shrinking (down to [MinTextScale]) when its width is too
 * narrow, and only wraps once that floor is reached.
 */
@Composable
private fun FitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) = FitText(AnnotatedString(text), style, modifier, textAlign)

@Composable
private fun FitText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    BoxWithConstraints(modifier = modifier) {
        val scale = rememberFitScale(listOf(FitLine(text, style)), maxWidth)
        Text(
            text = text,
            style = style.scaled(scale),
            textAlign = textAlign,
            modifier = if (textAlign != null) Modifier.fillMaxWidth() else Modifier
        )
    }
}

private data class FitLine(val text: AnnotatedString, val style: TextStyle) {
    constructor(text: String, style: TextStyle) : this(AnnotatedString(text), style)
}

/**
 * The scale (1 or less) at which the widest of [lines] fits [availableWidth] on one line.
 * The design packs three contact columns across a phone, so without this long numbers
 * would wrap on narrower screens and at larger font scales.
 */
@Composable
private fun rememberFitScale(
    lines: List<FitLine>,
    availableWidth: Dp,
    minScale: Float = MinTextScale
): Float {
    val measurer = rememberTextMeasurer()
    val availablePx = with(LocalDensity.current) { availableWidth.toPx() }
    return remember(lines, availablePx, minScale, measurer) {
        val widest = lines.maxOfOrNull { line ->
            measurer.measure(line.text, line.style, softWrap = false, maxLines = 1).size.width
        } ?: 0
        if (availablePx <= 0f || widest <= availablePx) {
            1f
        } else {
            // Slight margin so rounding in the final layout cannot push the text over.
            (availablePx / widest * 0.98f).coerceAtLeast(minScale)
        }
    }
}

private fun TextStyle.scaled(scale: Float): TextStyle =
    if (scale == 1f) this else copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

private const val MinTextScale = 0.8f
private const val SmallCallWidthFraction = 0.67f

private val CardShape = RoundedCornerShape(8.dp)
private val ButtonShape = RoundedCornerShape(5.dp)

private val ContactAvatarStart = 6.dp
private val ContactAvatarSize = 34.dp
private val ColumnEndPadding = 3.dp
private val ServiceGlyphStart = 8.dp
private val ServiceGlyphSize = 25.dp
private val ServiceGlyphGap = 6.dp

private val HeaderTitle = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = AlertInk)

private val BannerTitle = TextStyle(
    fontSize = 21.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.3.sp,
    color = AlertTitleRed
)
private val BannerDate = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, color = AlertGrey)
private val BannerHeadline = TextStyle(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, color = AlertHeadlineRed)
private val BannerMessage = TextStyle(fontSize = 11.5.sp, lineHeight = 14.sp, color = AlertInk)
private val SosLabel = TextStyle(fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, color = SurfaceWhite)

private val SenderName = TextStyle(fontSize = 20.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium, color = AlertInk)
private val SenderPhone = TextStyle(fontSize = 17.sp, lineHeight = 20.sp, color = AlertGrey)
private val SenderSubtitle = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, color = AlertGrey)
private val SenderCallLabel = TextStyle(fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = SurfaceWhite)

private val TrustedTitle = TextStyle(fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, color = AlertTrustedBlue)
private val HelpTitle = TextStyle(fontSize = 15.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, color = AlertInk)
private val SectionSubtitle = TextStyle(fontSize = 10.sp, lineHeight = 12.5.sp, color = AlertSubtitleGrey)

private val ContactName = TextStyle(fontSize = 11.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, color = AlertInk)
private val ContactDetail = TextStyle(fontSize = 11.sp, lineHeight = 13.5.sp, color = AlertInkSoft)
private val ChipLabel = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium)
private val SmallCallLabel = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = SurfaceWhite)

private val LocationTitle = TextStyle(fontSize = 14.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = AlertInk)
private val LocationAddress = TextStyle(fontSize = 12.sp, lineHeight = 13.5.sp, color = AlertGrey)
private val ViewMapLabel = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = AlertLinkBlue)

private val StatusLabel = TextStyle(fontSize = 11.5.sp, lineHeight = 14.sp, color = AlertInk)
private val StatusValue = TextStyle(fontSize = 18.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, color = AlertInk)

private val ServicesTitle = TextStyle(fontSize = 13.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium, color = AlertInk)
private val ServiceTitle = TextStyle(fontSize = 11.5.sp, lineHeight = 13.5.sp, fontWeight = FontWeight.Medium, color = AlertInk)
private val ServiceDetail = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, color = AlertInkSoft)
private val ServicePhone = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold, color = AlertInk)
private val Disclaimer = TextStyle(fontSize = 9.5.sp, lineHeight = 12.sp, color = AlertGrey)

private val EmergencyLabel = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, color = SurfaceWhite)

@Preview(showBackground = true, widthDp = 393, heightDp = 900)
@Composable
private fun SampleAlertScreenPreview() {
    PukaarAlertTheme {
        SampleAlertScreen(alert = AlertRepository.sampleAlert(), onBack = {})
    }
}

/** Narrow width plus a raised font scale — the case that makes the columns shrink their text. */
@Preview(showBackground = true, widthDp = 340, heightDp = 1000, fontScale = 1.3f)
@Composable
private fun SampleAlertScreenCompactPreview() {
    PukaarAlertTheme {
        SampleAlertScreen(alert = AlertRepository.sampleAlert(), onBack = {})
    }
}
