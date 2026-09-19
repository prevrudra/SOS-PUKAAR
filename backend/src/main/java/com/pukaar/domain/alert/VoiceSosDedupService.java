package com.pukaar.domain.alert;

import com.pukaar.common.PhoneNumbers;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Atomic claim: only one voice IVR per (event, phone). */
@Service
@RequiredArgsConstructor
public class VoiceSosDedupService {
    private final EntityManager em;

    @Transactional
    public boolean tryClaim(UUID eventId, String phoneE164) {
        if (eventId == null || phoneE164 == null || phoneE164.isBlank()) return false;
        String phone = PhoneNumbers.toE164(phoneE164);
        int inserted = em.createNativeQuery(
                        "INSERT INTO voice_sos_sent (event_id, phone_e164) VALUES (:eventId, :phone) "
                                + "ON CONFLICT (event_id, phone_e164) DO NOTHING")
                .setParameter("eventId", eventId)
                .setParameter("phone", phone)
                .executeUpdate();
        return inserted > 0;
    }

    @Transactional
    public boolean alreadySent(UUID eventId, String phoneE164) {
        if (eventId == null || phoneE164 == null || phoneE164.isBlank()) return false;
        String phone = PhoneNumbers.toE164(phoneE164);
        Number count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM voice_sos_sent WHERE event_id = :eventId AND phone_e164 = :phone")
                .setParameter("eventId", eventId)
                .setParameter("phone", phone)
                .getSingleResult();
        return count != null && count.longValue() > 0;
    }
}
