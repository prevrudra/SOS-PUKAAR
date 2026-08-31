package com.pukaar.domain.emergency;

import com.pukaar.common.*;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.contact.TrustedContactEntity;
import com.pukaar.domain.contact.TrustedContactRepository;
import com.pukaar.domain.hospital.HospitalEntity;
import com.pukaar.domain.hospital.HospitalRepository;
import com.pukaar.domain.evidence.AudioSegmentEntity;
import com.pukaar.domain.evidence.AudioSegmentRepository;
import com.pukaar.domain.notification.NotificationService;
import com.pukaar.domain.police.PoliceStationEntity;
import com.pukaar.domain.police.PoliceStationRepository;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyOrchestrator {
    private final EmergencyEventRepository eventRepo;
    private final EmergencyLocationRepository locationRepo;
    private final ContactDeliveryRepository deliveryRepo;
    private final EmergencyAuditRepository auditRepo;
    private final AudioSegmentRepository audioRepo;
    private final TrustedContactRepository contactRepo;
    private final PoliceStationRepository policeRepo;
    private final HospitalRepository hospitalRepo;
    private final UserRepository userRepo;
    private final MockDrillRepository mockDrillRepo;
    private final NotificationService notificationService;
    private final PukaarProperties props;

    @Transactional
    public Map<String, Object> trigger(UUID userId, TriggerType triggerType, Double lat, Double lng,
                                       Double accuracy, boolean mockDrill) {
        var existing = eventRepo.findFirstByUserIdAndClosedAtIsNullOrderByStartedAtDesc(userId);
        if (existing.isPresent()) {
            EmergencyEventEntity active = existing.get();
            // Auto-close stale sessions (older than configured timeout)
            Instant staleBefore = Instant.now().minusSeconds(props.getEmergency().getSessionTimeoutHours() * 3600L);
            boolean stale = active.getStartedAt() != null && active.getStartedAt().isBefore(staleBefore);
            if (stale || mockDrill) {
                active.setClosureReason(ClosureReason.SYSTEM_TIMEOUT);
                active.setClosedAt(Instant.now());
                active.setStatus(EmergencyStatus.CLOSED);
                eventRepo.save(active);
                audit(active.getId(), userId, "AUTO_CLOSED_FOR_NEW_TRIGGER", Map.of(
                        "stale", stale,
                        "mockDrill", mockDrill
                ));
            } else {
                // Resume existing emergency instead of failing with HTTP 400
                if (lat != null && lng != null) {
                    applyLocation(active, lat, lng, accuracy);
                }
                Map<String, Object> dto = toEventDto(active, true);
                dto.put("resumed", true);
                return dto;
            }
        }

        UserEntity user = userRepo.findById(userId).orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        EmergencyEventEntity event = EmergencyEventEntity.builder()
                .userId(userId)
                .triggerType(triggerType)
                .mockDrill(mockDrill)
                .status(EmergencyStatus.TRIGGERED)
                .build();
        event = eventRepo.save(event);
        audit(event.getId(), userId, "TRIGGERED", Map.of("trigger", triggerType.name(), "mock", mockDrill));

        if (lat != null && lng != null) {
            applyLocation(event, lat, lng, accuracy);
        }

        notifyContacts(user, event);
        event.setStatus(EmergencyStatus.CONTACTS_NOTIFIED);
        event = eventRepo.save(event);
        audit(event.getId(), userId, "CONTACTS_NOTIFIED", Map.of());

        if (!mockDrill && triggerType != TriggerType.HELP) {
            event.setStatus(EmergencyStatus.AUDIO_RECORDING_ACTIVE);
            event.setCall112Status(Call112Status.INITIATED);
            event.setStatus(EmergencyStatus.WAITING_SAFE);
            event = eventRepo.save(event);
            audit(event.getId(), userId, "AUDIO_AND_112_PATHWAY", Map.of("call112", "INITIATED"));
        } else {
            event.setStatus(EmergencyStatus.WAITING_SAFE);
            event = eventRepo.save(event);
        }

        UUID drillId = null;
        if (mockDrill) {
            MockDrillEntity drill = MockDrillEntity.builder()
                    .userId(userId)
                    .eventId(event.getId())
                    .result(MockDrillResult.IN_PROGRESS)
                    .locationOk(lat != null)
                    .contactsOk(!deliveryRepo.findByEventId(event.getId()).isEmpty())
                    .permissionsOk(true)
                    .build();
            drill = mockDrillRepo.save(drill);
            drillId = drill.getId();
        }

        Map<String, Object> dto = toEventDto(event, true);
        if (drillId != null) {
            dto.put("mockDrillId", drillId);
        }
        return dto;
    }

    @Transactional
    public Map<String, Object> updateLocation(UUID userId, UUID eventId, double lat, double lng, Double accuracy) {
        EmergencyEventEntity event = requireOwnedActive(userId, eventId);
        applyLocation(event, lat, lng, accuracy);
        event.setStatus(EmergencyStatus.LIVE_LOCATION_ACTIVE);
        eventRepo.save(event);
        return toEventDto(event, false);
    }

    @Transactional
    public Map<String, Object> markSafe(UUID userId, UUID eventId, ClosureReason reason) {
        EmergencyEventEntity event = requireOwnedActive(userId, eventId);
        event.setClosureReason(reason == null ? ClosureReason.IM_SAFE : reason);
        event.setClosedAt(Instant.now());
        event.setStatus(EmergencyStatus.CLOSED);
        eventRepo.save(event);
        audit(event.getId(), userId, "CLOSED", Map.of("reason", event.getClosureReason().name()));
        notificationService.notifyEmergencyClosed(event.getId());
        return toEventDto(event, true);
    }

    @Transactional
    public Map<String, Object> registerAudioSegment(UUID userId, UUID eventId, int index, String checksum, Long byteSize) {
        EmergencyEventEntity event = requireOwnedActive(userId, eventId);
        AudioSegmentEntity segment = AudioSegmentEntity.builder()
                .eventId(event.getId())
                .segmentIndex(index)
                .durationSec(props.getEmergency().getAudioSegmentSeconds())
                .checksumSha256(checksum)
                .byteSize(byteSize)
                .uploadStatus(UploadStatus.PENDING)
                .build();
        segment = audioRepo.save(segment);
        event.setStatus(EmergencyStatus.SEGMENTS_UPLOADING);
        eventRepo.save(event);
        audit(event.getId(), userId, "AUDIO_SEGMENT_CREATED", Map.of("index", index, "segmentId", segment.getId().toString()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("segmentId", segment.getId());
        m.put("uploadStatus", segment.getUploadStatus());
        m.put("cloudSafe", false);
        m.put("message", "Segment not cloud-safe until upload succeeds");
        return m;
    }

    @Transactional
    public Map<String, Object> markSegmentUploaded(UUID userId, UUID eventId, UUID segmentId, String storageKey) {
        requireOwnedActive(userId, eventId);
        AudioSegmentEntity segment = audioRepo.findById(segmentId)
                .orElseThrow(() -> new ApiException("SEGMENT_NOT_FOUND", "Audio segment not found"));
        if (!segment.getEventId().equals(eventId)) {
            throw new ApiException("SEGMENT_MISMATCH", "Segment does not belong to event");
        }
        segment.setStorageKey(storageKey);
        segment.setUploadStatus(UploadStatus.UPLOADED);
        segment.setUploadedAt(Instant.now());
        audioRepo.save(segment);
        audit(eventId, userId, "AUDIO_SEGMENT_UPLOADED", Map.of("segmentId", segmentId.toString()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("segmentId", segment.getId());
        m.put("uploadStatus", segment.getUploadStatus());
        m.put("cloudSafe", true);
        m.put("message", "Evidence segment is retrievable from secure storage");
        return m;
    }

    @Transactional
    public Map<String, Object> completeMockDrill(UUID userId, UUID drillId, boolean contactsConfirmed, String notes) {
        MockDrillEntity drill = (drillId != null
                ? mockDrillRepo.findById(drillId)
                : mockDrillRepo.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .orElseThrow(() -> new ApiException("DRILL_NOT_FOUND", "Mock drill not found"));
        if (!drill.getUserId().equals(userId)) throw new ApiException("FORBIDDEN", "Not your drill");

        List<TrustedContactEntity> contacts = contactRepo.findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(userId);
        long verifiedCount = contacts.stream().filter(TrustedContactEntity::isVerified).count();
        boolean contactsOk = contacts.size() >= 2 && contacts.size() <= 3 && verifiedCount >= 2;
        drill.setContactsOk(contactsOk);
        if (!contactsOk) {
            drill.setFailureNotes("Add 2–3 trusted contacts and verify each with the code sent to them.");
        }

        drill.setConfirmedByUser(contactsConfirmed);
        if (notes != null && !notes.isBlank()) {
            drill.setFailureNotes(notes);
        }
        if (!contactsOk) {
            throw new ApiException("CONTACTS_REQUIRED", "Add and verify at least 2 trusted contacts (max 3) before completing the drill");
        }
        boolean pass = Boolean.TRUE.equals(drill.getLocationOk())
                && Boolean.TRUE.equals(drill.getContactsOk())
                && Boolean.TRUE.equals(drill.getPermissionsOk())
                && contactsConfirmed;
        drill.setResult(pass ? MockDrillResult.PASS : MockDrillResult.FAIL);
        drill.setCompletedAt(Instant.now());
        mockDrillRepo.save(drill);

        if (drill.getEventId() != null) {
            eventRepo.findById(drill.getEventId()).ifPresent(e -> {
                e.setClosedAt(Instant.now());
                e.setClosureReason(ClosureReason.SYSTEM_TIMEOUT);
                e.setStatus(EmergencyStatus.CLOSED);
                eventRepo.save(e);
            });
        }

        UserEntity user = userRepo.findById(userId).orElseThrow();
        user.setMockDrillPassed(pass);
        if (pass) {
            user.setProtectionReady(true);
        }
        userRepo.save(user);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("result", drill.getResult());
        m.put("protectionReady", user.isProtectionReady());
        m.put("failureNotes", drill.getFailureNotes());
        return m;
    }

    public Map<String, Object> getActive(UUID userId) {
        return eventRepo.findFirstByUserIdAndClosedAtIsNullOrderByStartedAtDesc(userId)
                .map(e -> toEventDto(e, true))
                .orElse(Map.of("active", false));
    }

    public Map<String, Object> getEvent(UUID userId, UUID eventId) {
        EmergencyEventEntity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException("EVENT_NOT_FOUND", "Emergency not found"));
        if (!event.getUserId().equals(userId)) throw new ApiException("FORBIDDEN", "Not your event");
        return toEventDto(event, true);
    }

    private void applyLocation(EmergencyEventEntity event, double lat, double lng, Double accuracy) {
        event.setLatitude(lat);
        event.setLongitude(lng);
        event.setLocationAccuracyM(accuracy);
        event.setLocationAcquiredAt(Instant.now());
        if (event.getStatus() == EmergencyStatus.TRIGGERED) {
            event.setStatus(EmergencyStatus.LOCATION_ACQUIRED);
        }
        locationRepo.save(EmergencyLocationEntity.builder()
                .eventId(event.getId())
                .latitude(lat)
                .longitude(lng)
                .accuracyM(accuracy)
                .build());
        policeRepo.findNearest(lat, lng, 1).stream().findFirst().ifPresent(station -> {
            event.setPoliceStationId(station.getId());
            audit(event.getId(), event.getUserId(), "POLICE_RESOLVED", Map.of(
                    "stationId", station.getId().toString(),
                    "verified", station.isPhoneVerified()
            ));
        });
        eventRepo.save(event);
        audit(event.getId(), event.getUserId(), "LOCATION_UPDATED", Map.of("lat", lat, "lng", lng));
    }

    private void notifyContacts(UserEntity user, EmergencyEventEntity event) {
        List<ContactRole> roles = event.getTriggerType() == TriggerType.HELP
                ? List.of(ContactRole.HELP_MONITOR, ContactRole.HELP_BACKUP, ContactRole.SOS_TRUSTED)
                : List.of(ContactRole.SOS_TRUSTED);
        List<TrustedContactEntity> contacts = contactRepo.findByOwnerUserIdAndContactRoleInAndActiveTrue(user.getId(), roles);
        if (contacts.isEmpty()) {
            contacts = contactRepo.findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(user.getId());
        }
        for (TrustedContactEntity c : contacts) {
            ContactDeliveryEntity delivery = ContactDeliveryEntity.builder()
                    .eventId(event.getId())
                    .contactId(c.getId())
                    .contactName(c.getName())
                    .contactPhone(c.getPhoneE164())
                    .status(DeliveryStatus.PENDING)
                    .build();
            delivery = deliveryRepo.save(delivery);
            UUID userId = user.getId();
            UUID eventId = event.getId();
            UUID deliveryId = delivery.getId();
            // Fire after commit so async thread can load the persisted delivery row
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.enqueueEmergencyAlert(userId, eventId, deliveryId);
                    }
                });
            } else {
                notificationService.enqueueEmergencyAlert(userId, eventId, deliveryId);
            }
        }
    }

    private EmergencyEventEntity requireOwnedActive(UUID userId, UUID eventId) {
        EmergencyEventEntity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException("EVENT_NOT_FOUND", "Emergency not found"));
        if (!event.getUserId().equals(userId)) throw new ApiException("FORBIDDEN", "Not your event");
        if (event.getClosedAt() != null) throw new ApiException("EVENT_CLOSED", "Emergency already closed");
        return event;
    }

    private void audit(UUID eventId, UUID actor, String action, Map<String, Object> detail) {
        auditRepo.save(EmergencyAuditEntity.builder()
                .eventId(eventId)
                .actorUserId(actor)
                .action(action)
                .detail(detail)
                .build());
    }

    public Map<String, Object> toEventDto(EmergencyEventEntity event, boolean includeDetails) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("active", event.getClosedAt() == null);
        m.put("id", event.getId());
        m.put("triggerType", event.getTriggerType());
        m.put("status", event.getStatus());
        m.put("mockDrill", event.isMockDrill());
        m.put("latitude", event.getLatitude());
        m.put("longitude", event.getLongitude());
        m.put("call112Status", event.getCall112Status());
        m.put("closureReason", event.getClosureReason());
        m.put("startedAt", event.getStartedAt());
        m.put("closedAt", event.getClosedAt());
        if (includeDetails) {
            userRepo.findById(event.getUserId()).ifPresent(u -> {
                m.put("userName", u.getFullName());
                m.put("userPhone", u.getPhoneE164());
            });
            m.put("deliveries", deliveryRepo.findByEventId(event.getId()).stream().map(d -> {
                Map<String, Object> dm = new LinkedHashMap<>();
                dm.put("name", d.getContactName());
                dm.put("phone", d.getContactPhone());
                dm.put("status", d.getStatus());
                dm.put("acknowledgedAt", d.getAcknowledgedAt());
                return dm;
            }).toList());
            m.put("audioSegments", audioRepo.findByEventIdOrderBySegmentIndexAsc(event.getId()).stream().map(s -> {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("id", s.getId());
                sm.put("index", s.getSegmentIndex());
                sm.put("uploadStatus", s.getUploadStatus());
                sm.put("cloudSafe", s.getUploadStatus() == UploadStatus.UPLOADED);
                return sm;
            }).toList());
            if (event.getPoliceStationId() != null) {
                policeRepo.findById(event.getPoliceStationId()).ifPresent(p -> m.put("policeStation", toPoliceDto(p)));
            } else if (event.getLatitude() != null && event.getLongitude() != null) {
                policeRepo.findNearest(event.getLatitude(), event.getLongitude(), 1).stream()
                        .findFirst()
                        .ifPresent(p -> m.put("policeStation", toPoliceDto(p)));
            }
            if (event.getLatitude() != null && event.getLongitude() != null) {
                hospitalRepo.findNearest(event.getLatitude(), event.getLongitude(), 1).stream()
                        .findFirst()
                        .ifPresent(h -> m.put("nearestHospital", toHospitalDto(h)));
            }
            m.put("audit", auditRepo.findByEventIdOrderByCreatedAtAsc(event.getId()).stream().map(a -> {
                Map<String, Object> am = new LinkedHashMap<>();
                am.put("action", a.getAction());
                am.put("detail", a.getDetail());
                am.put("at", a.getCreatedAt());
                return am;
            }).toList());
        }
        return m;
    }

    private Map<String, Object> toPoliceDto(PoliceStationEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("address", p.getAddress());
        m.put("source", p.getSource());
        m.put("phoneVerified", p.isPhoneVerified());
        if (p.isPhoneVerified()) {
            m.put("phone", p.getPhoneE164());
        }
        m.put("latitude", p.getLatitude());
        m.put("longitude", p.getLongitude());
        return m;
    }

    private Map<String, Object> toHospitalDto(HospitalEntity h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("name", h.getName());
        m.put("address", h.getAddress());
        m.put("phone", h.getPhoneE164());
        m.put("latitude", h.getLatitude());
        m.put("longitude", h.getLongitude());
        return m;
    }
}
