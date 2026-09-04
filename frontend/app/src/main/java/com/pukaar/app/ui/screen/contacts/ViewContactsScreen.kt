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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary

@Composable
fun ViewContactsScreen(
    contacts: List<ContactUiModel>,
    onBack: () -> Unit,
    onAddContact: () -> Unit,
    onEditContact: (ContactUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = Black,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContact,
                containerColor = PukaarRed,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_contact_title))
            }
        }
    ) { padding ->
        PukaarScreen(
            title = stringResource(R.string.view_contacts_title),
            onBack = onBack,
            modifier = Modifier.padding(padding)
        ) {
            Text(
                text = stringResource(R.string.view_contacts_subtitle),
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

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
                        onEditContact = onEditContact
                    )
                }
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun ServiceSection(
    type: ContactType,
    contacts: List<ContactUiModel>,
    onEditContact: (ContactUiModel) -> Unit,
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
                    ContactRow(contact = contact, onClick = { onEditContact(contact) })
                    if (index != contacts.lastIndex) RowDivider()
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (contact.verified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    VerifiedBadge()
                }
            }
            Text(text = contact.phoneNumber, color = TextTertiary, fontSize = 11.sp)
            if (contact.relationship.isNotBlank()) {
                Text(
                    text = contact.relationship,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            if (contact.notes.isNotBlank()) {
                Text(
                    text = contact.notes,
                    color = TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun VerifiedBadge() {
    Box(
        modifier = Modifier
            .background(SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(R.string.contact_verified).uppercase(),
            color = SuccessGreen,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
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
                ContactUiModel("1", "Son", "+91 98765 43210", ContactType.SOS, "Son", verified = true),
                ContactUiModel("2", "Doctor", "+91 98765 43218", ContactType.DOCTOR, "Family Doctor", notes = "Cardiologist")
            ),
            onBack = {},
            onAddContact = {},
            onEditContact = {}
        )
    }
}
