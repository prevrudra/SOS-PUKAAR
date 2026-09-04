package com.pukaar.domain.alert;

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
                : event.getTriggerType() == TriggerType.HELP ? "PUKAAR HELP REQUEST" : "PUKAAR EMERGENCY SOS";
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("\n");
        sb.append(who).append(" (").append(phone).append(")\n");
        if (event.isMockDrill()) {
            sb.append("This is a practice drill.\n");
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
            if (police.getPhoneE164() != null) sb.append(" ").append(police.getPhoneE164());
            sb.append("\n");
        }
        if (hospital != null) {
            sb.append("Hospital: ").append(hospital.getName());
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
        if (event.isMockDrill()) return "PUKAAR TEST ALERT";
        return event.getTriggerType() == TriggerType.HELP ? "PUKAAR HELP ALERT" : "PUKAAR EMERGENCY SOS";
    }

    public String buildPushBody(UserEntity user, EmergencyEventEntity event) {
        String who = user.getFullName() != null ? user.getFullName() : "A PUKAAR user";
        if (event.isMockDrill()) return who + " — practice drill. Tap to view.";
        if (event.getTriggerType() == TriggerType.HELP) return who + " pressed HELP. Tap to view location.";
        return who + " may be in danger. Tap NOW for location and details.";
    }

    public String contactsSummary(List<TrustedContactEntity> contacts) {
        return contacts.stream()
                .limit(5)
                .map(c -> c.getName() + " " + c.getPhoneE164())
                .collect(Collectors.joining(", "));
    }
}
