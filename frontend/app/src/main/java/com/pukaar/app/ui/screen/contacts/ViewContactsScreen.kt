package com.pukaar.app.ui.screen.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary

/**
 * Menu item: the people an alert reaches.
 *
 * One section per category, in [ContactType] order, all on screen together — so
 * "who gets my SOS?" is answered by looking rather than by filtering. Each contact
 * belongs to exactly one category, so every name appears under a single heading.
 */
@Composable
fun ViewContactsScreen(
    contacts: List<ContactUiModel>,
    onBack: () -> Unit,
    onContactClick: (ContactUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarScreen(
        title = stringResource(R.string.view_contacts_title),
        onBack = onBack,
        modifier = modifier
    ) {
        if (contacts.isEmpty()) {
            EmptyState(stringResource(R.string.view_contacts_empty))
            return@PukaarScreen
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ContactType.entries.forEach { type ->
                ServiceSection(
                    type = type,
                    contacts = contacts.filterByType(type),
                    onContactClick = onContactClick
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ServiceSection(
    type: ContactType,
    contacts: List<ContactUiModel>,
    onContactClick: (ContactUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = type.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(type.sectionRes).uppercase(),
                color = type.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        SectionCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
            if (contacts.isEmpty()) {
                Text(
                    text = stringResource(R.string.contact_section_empty),
                    color = TextTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            } else {
                contacts.forEachIndexed { index, contact ->
                    ContactRow(contact = contact, onClick = { onContactClick(contact) })
                    if (index != contacts.lastIndex) {
                        RowDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar()
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name, color = TextPrimary, fontSize = 13.sp)
            Text(text = contact.phoneNumber, color = TextTertiary, fontSize = 11.sp)
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ContactAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(SurfaceElevated, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun ViewContactsScreenPreview() {
    PukaarTheme {
        ViewContactsScreen(
            contacts = listOf(
                ContactUiModel("1", "Son", "+91 98765 43210", ContactType.SOS),
                ContactUiModel("2", "Spouse", "+91 98765 43211", ContactType.SOS),
                ContactUiModel("3", "Neighbour", "+91 98765 43212", ContactType.SOS),
                ContactUiModel("4", "Daughter", "+91 98765 43213", ContactType.HELP),
                ContactUiModel("5", "Brother", "+91 98765 43214", ContactType.HELP),
                ContactUiModel("6", "Best Friend", "+91 98765 43215", ContactType.HELP),
                ContactUiModel("7", "Sister", "+91 98765 43216", ContactType.INACTIVITY),
                ContactUiModel("8", "Caretaker", "+91 98765 43217", ContactType.INACTIVITY),
                ContactUiModel("9", "Family Doctor", "+91 98765 43218", ContactType.INACTIVITY)
            ),
            onBack = {},
            onContactClick = {}
        )
    }
}
