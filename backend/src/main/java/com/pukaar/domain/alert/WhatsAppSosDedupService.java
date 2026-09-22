package com.pukaar.domain.alert;

import jakarta.persistence.EntityManager;
import com.pukaar.common.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Atomic claim: only the first delivery attempt per (event, phone) may send WhatsApp.
 */
@Service
@RequiredArgsConstructor
public class WhatsAppSosDedupService {
    private final EntityManager em;

    /** @return true if this caller won the right to send (insert succeeded). */
    @Transactional
    public boolean tryClaim(UUID eventId, String phoneE164) {
        if (eventId == null || phoneE164 == null || phoneE164.isBlank()) return false;
        String phone = PhoneNumbers.toE164(phoneE164);
        int inserted = em.createNativeQuery(
                        "INSERT INTO whatsapp_sos_sent (event_id, phone_e164) VALUES (:eventId, :phone) "
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
                        "SELECT COUNT(*) FROM whatsapp_sos_sent WHERE event_id = :eventId AND phone_e164 = :phone")
                .setParameter("eventId", eventId)
                .setParameter("phone", phone)
                .getSingleResult();
        return count != null && count.longValue() > 0;
    }

    /** Free the claim when Meta rejects the send so a retry / fallback can try again. */
    @Transactional
    public void releaseClaim(UUID eventId, String phoneE164) {
        if (eventId == null || phoneE164 == null || phoneE164.isBlank()) return;
        String phone = PhoneNumbers.toE164(phoneE164);
        em.createNativeQuery(
                        "DELETE FROM whatsapp_sos_sent WHERE event_id = :eventId AND phone_e164 = :phone")
                .setParameter("eventId", eventId)
                .setParameter("phone", phone)
                .executeUpdate();
    }
}
