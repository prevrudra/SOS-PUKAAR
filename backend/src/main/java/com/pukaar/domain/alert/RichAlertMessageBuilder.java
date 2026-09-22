package com.pukaar.domain.alert;

import com.pukaar.common.InactivityLevel;
import com.pukaar.common.TriggerType;
import com.pukaar.domain.contact.TrustedContactEntity;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.hospital.HospitalEntity;
import com.pukaar.domain.police.PoliceStationEntity;
import com.pukaar.domain.user.UserEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RichAlertMessageBuilder {

    public String buildSmsBody(
            UserEntity user,
            EmergencyEventEntity event,
            List<TrustedContactEntity> allContacts,
            PoliceStationEntity police,
            HospitalEntity hospital,
            String ambulanceNumber
    ) {
        String who = user.getFullName() != null ? user.getFullName() : "PUKAAR user";
        String phone = user.getPhoneE164();
        String prefix = event.isMockDrill() ? "PUKAAR TEST ALERT"
                : event.getTriggerType() == TriggerType.INACTIVITY ? "PUKAAR INACTIVITY ALERT"
                : event.getTriggerType() == TriggerType.HELP ? "PUKAAR HELP REQUEST" : "PUKAAR EMERGENCY SOS";
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("\n");
        sb.append(who).append(" (").append(phone).append(")\n");
        if (event.isMockDrill()) {
            sb.append("This is a practice drill.\n");
        } else if (event.getTriggerType() == TriggerType.INACTIVITY) {
            sb.append("Has had no activity for their selected period — please check on them.\n");
        } else if (event.getTriggerType() == TriggerType.HELP) {
            sb.append("Needs assistance — call immediately.\n");
        } else {
            sb.append("MAY BE IN DANGER — call immediately.\n");
        }
        if (event.getLatitude() != null && event.getLongitude() != null) {
            sb.append("Location: https://maps.google.com/?q=")
                    .append(event.getLatitude()).append(",").append(event.getLongitude()).append("\n");
        } else {
            sb.append("Location: not available yet — call now.\n");
        }
        if (event.getBatteryPct() != null) {
            sb.append("Battery: ").append(event.getBatteryPct()).append("%\n");
        }
        if (event.getNetworkType() != null && !event.getNetworkType().isBlank()) {
            sb.append("Network: ").append(event.getNetworkType()).append("\n");
        }
        if (!allContacts.isEmpty()) {
            sb.append("Other contacts:\n");
            for (TrustedContactEntity c : allContacts.stream().limit(5).toList()) {
                sb.append("- ").append(c.getName()).append(" ").append(c.getPhoneE164()).append("\n");
            }
        }
        sb.append("Emergency: 112\n");
        if (ambulanceNumber != null && !ambulanceNumber.isBlank()) {
            sb.append("Ambulance: ").append(ambulanceNumber).append("\n");
        }
        if (police != null) {
            sb.append("Police: ").append(police.getName());
            if (police.getAddress() != null && !police.getAddress().isBlank()) {
                sb.append(" — ").append(police.getAddress());
            }
            if (police.getPhoneE164() != null) sb.append(" ").append(police.getPhoneE164());
            sb.append("\n");
        }
        if (hospital != null) {
            sb.append("Hospital: ").append(hospital.getName());
            if (hospital.getAddress() != null && !hospital.getAddress().isBlank()) {
                sb.append(" — ").append(hospital.getAddress());
            }
            if (hospital.getPhoneE164() != null) sb.append(" ").append(hospital.getPhoneE164());
            sb.append("\n");
        }
        sb.append("Open PUKAAR High Alert app for live updates.");
        return sb.toString().trim();
    }

    public String buildWhatsAppBody(
            UserEntity user,
            EmergencyEventEntity event,
            List<TrustedContactEntity> allContacts,
            PoliceStationEntity police,
            HospitalEntity hospital,
            String ambulanceNumber
    ) {
        return buildSmsBody(user, event, allContacts, police, hospital, ambulanceNumber);
    }

    public String buildPushTitle(EmergencyEventEntity event) {
        return buildPushTitle(event, null);
    }

    public String buildPushTitle(EmergencyEventEntity event, InactivityLevel level) {
        if (event.isMockDrill()) return "PUKAAR TEST ALERT";
        if (event.getTriggerType() == TriggerType.INACTIVITY) {
            return "PUKAAR INACTIVITY ALERT";
        }
        return event.getTriggerType() == TriggerType.HELP
                ? "PUKAAR HELP — EMERGENCY"
                : "PUKAAR SOS — EMERGENCY";
    }

    public String buildPushBody(UserEntity user, EmergencyEventEntity event) {
        return buildPushBody(user, event, null);
    }

    public String buildPushBody(UserEntity user, EmergencyEventEntity event, InactivityLevel level) {
        String who = user.getFullName() != null ? user.getFullName() : "A PUKAAR user";
        if (event.isMockDrill()) {
            return who + " has activated a practice SOS. Tap for live location.";
        }
        if (event.getTriggerType() == TriggerType.INACTIVITY) {
            return who + " has had no activity for their selected period. Please check on them.";
        }
        if (event.getTriggerType() == TriggerType.HELP) {
            return who + " has activated HELP and may need assistance. Tap for location.";
        }
        return who + " has activated SOS and may need immediate help. Tap NOW for location.";
    }

    public String contactsSummary(List<TrustedContactEntity> contacts) {
        return contacts.stream()
                .limit(5)
                .map(c -> c.getName() + " " + c.getPhoneE164())
                .collect(Collectors.joining(", "));
    }
}
