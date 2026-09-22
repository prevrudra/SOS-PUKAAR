package com.pukaar.domain.emergency;

import com.pukaar.common.*;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.contact.TrustedContactEntity;
import com.pukaar.domain.contact.TrustedContactRepository;
import com.pukaar.domain.hospital.HospitalEntity;
import com.pukaar.domain.hospital.HospitalRepository;
import com.pukaar.domain.nearby.NearbyPlacesService;
import com.pukaar.domain.evidence.AudioSegmentEntity;
import com.pukaar.domain.evidence.AudioSegmentRepository;
import com.pukaar.domain.evidence.EvidenceStorageService;
import com.pukaar.domain.alert.DeliveryStatusService;
import com.pukaar.domain.alert.LocationUpdateNotifier;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final EvidenceStorageService evidenceStorage;
    private final TrustedContactRepository contactRepo;
    private final PoliceStationRepository policeRepo;
    private final HospitalRepository hospitalRepo;
    private final UserRepository userRepo;
    private final MockDrillRepository mockDrillRepo;
    private final NotificationService notificationService;
    private final PukaarProperties props;
    private final NearbyPlacesService nearbyPlacesService;
    private final LocationUpdateNotifier locationUpdateNotifier;
    private final DeliveryStatusService deliveryStatusService;

    @Transactional
    public Map<String, Object> trigger(UUID userId, TriggerType triggerType, Double lat, Double lng,
                                       Double accuracy, boolean mockDrill, Integer batteryPct, String networkType) {
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
                // Resume existing emergency — re-fire any undelivered alerts
                if (lat != null && lng != null) {
                    applyLocation(active, lat, lng, accuracy);
                }
                reNotifyFailedDeliveries(active);
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
                .batteryPct(batteryPct)
                .networkType(networkType)
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

        if (!mockDrill && triggerType != TriggerType.HELP && triggerType != TriggerType.INACTIVITY) {
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
    public Map<String, Object> updateTelemetry(UUID userId, UUID eventId, Integer batteryPct, String networkType) {
        EmergencyEventEntity event = requireOwnedActive(userId, eventId);
        if (batteryPct != null) event.setBatteryPct(batteryPct);
        if (networkType != null) event.setNetworkType(networkType);
        eventRepo.save(event);
        return toEventDto(event, true);
    }

    @Transactional
    public Map<String, Object> updateLocation(UUID userId, UUID eventId, double lat, double lng, Double accuracy) {
        EmergencyEventEntity event = requireOwnedActive(userId, eventId);
        applyLocation(event, lat, lng, accuracy);
        event.setStatus(EmergencyStatus.LIVE_LOCATION_ACTIVE);
        eventRepo.save(event);
        locationUpdateNotifier.maybeNotify(eventId, event);
        return toEventDto(event, true);
    }

    @Transactional
    public Map<String, Object> markSafe(UUID userId, UUID eventId, ClosureReason reason) {
        EmergencyEventEntity event = requireOwnedActive(userId, eventId);
        event.setClosureReason(reason == null ? ClosureReason.IM_SAFE : reason);
        event.setClosedAt(Instant.now());
        event.setStatus(EmergencyStatus.CLOSED);
        eventRepo.save(event);
        locationUpdateNotifier.clear(eventId);
        audit(event.getId(), userId, "CLOSED", Map.of("reason", event.getClosureReason().name()));
        UUID closedId = event.getId();
        // Notify contacts only after DB commit — synchronous so the API reflects real delivery.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deliveryStatusService.notifyContactsUserSafe(closedId);
            }
        });
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
    public Map<String, Object> uploadAudioSegment(UUID userId, UUID eventId, UUID segmentId, MultipartFile file) {
        requireOwnedActive(userId, eventId);
        AudioSegmentEntity segment = audioRepo.findById(segmentId)
                .orElseThrow(() -> new ApiException("SEGMENT_NOT_FOUND", "Audio segment not found"));
        if (!segment.getEventId().equals(eventId)) {
            throw new ApiException("SEGMENT_MISMATCH", "Segment does not belong to event");
        }
        if (file == null || file.isEmpty()) {
            throw new ApiException("EMPTY_FILE", "Audio file is empty");
        }
        segment.setUploadStatus(UploadStatus.UPLOADING);
        audioRepo.save(segment);
        try {
            String storageKey = evidenceStorage.store(eventId, segmentId, file);
            Path stored = Paths.get(evidenceStoragePath()).resolve(storageKey);
            String checksum = HashUtil.sha256(Files.newInputStream(stored));
            if (segment.getChecksumSha256() != null
                    && !segment.getChecksumSha256().equalsIgnoreCase(checksum)) {
                Files.deleteIfExists(stored);
                throw new ApiException("CHECKSUM_MISMATCH", "Uploaded file checksum does not match");
            }
            segment.setStorageKey(storageKey);
            segment.setByteSize(Files.size(stored));
            if (segment.getChecksumSha256() == null) {
                segment.setChecksumSha256(checksum);
            }
            segment.setUploadStatus(UploadStatus.UPLOADED);
            segment.setUploadedAt(Instant.now());
            audioRepo.save(segment);
            audit(eventId, userId, "AUDIO_SEGMENT_UPLOADED", Map.of(
                    "segmentId", segmentId.toString(),
                    "storageKey", storageKey
            ));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("segmentId", segment.getId());
            m.put("uploadStatus", segment.getUploadStatus());
            m.put("cloudSafe", true);
            m.put("message", "Evidence segment stored on server");
            return m;
        } catch (ApiException e) {
            segment.setUploadStatus(UploadStatus.FAILED);
            audioRepo.save(segment);
            throw e;
        } catch (IOException e) {
            segment.setUploadStatus(UploadStatus.FAILED);
            audioRepo.save(segment);
            throw new ApiException("UPLOAD_FAILED", "Could not store audio evidence");
        }
    }

    private String evidenceStoragePath() {
        return props.getStorage().getLocalPath();
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

    @Transactional
    public Map<String, Object> getActive(UUID userId) {
        return eventRepo.findFirstByUserIdAndClosedAtIsNullOrderByStartedAtDesc(userId)
                .map(e -> {
                    if (closeIfStaleMockDrill(e, userId)) {
                        return Map.<String, Object>of("active", false);
                    }
                    return toEventDto(e, true);
                })
                .orElse(Map.of("active", false));
    }

    @Transactional
    public Map<String, Object> getEvent(UUID userId, UUID eventId) {
        EmergencyEventEntity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException("EVENT_NOT_FOUND", "Emergency not found"));
        if (!event.getUserId().equals(userId)) throw new ApiException("FORBIDDEN", "Not your event");
        closeIfStaleMockDrill(event, userId);
        return toEventDto(event, true);
    }

    private boolean closeIfStaleMockDrill(EmergencyEventEntity e, UUID userId) {
        if (!e.isMockDrill() || e.getClosedAt() != null || e.getStartedAt() == null) return false;
        if (!e.getStartedAt().isBefore(Instant.now().minus(10, java.time.temporal.ChronoUnit.MINUTES))) {
            return false;
        }
        e.setClosureReason(ClosureReason.IM_SAFE);
        e.setClosedAt(Instant.now());
        e.setStatus(EmergencyStatus.CLOSED);
        eventRepo.save(e);
        audit(e.getId(), userId, "AUTO_CLOSED_STALE_DRILL", Map.of());
        return true;
    }

    public Map<String, Object> listHistory(UUID userId) {
        List<EmergencyEventEntity> events = eventRepo.findByUserIdOrderByStartedAtDesc(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", events.stream().map(e -> toEventDto(e, true)).toList());
        m.put("total", events.size());
        return m;
    }

    public org.springframework.core.io.Resource streamOwnedAudio(UUID userId, UUID eventId, UUID segmentId) {
        EmergencyEventEntity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException("EVENT_NOT_FOUND", "Emergency not found"));
        if (!event.getUserId().equals(userId)) {
            throw new ApiException("FORBIDDEN", "Not your event");
        }
        AudioSegmentEntity segment = audioRepo.findById(segmentId)
                .orElseThrow(() -> new ApiException("SEGMENT_NOT_FOUND", "Audio segment not found"));
        if (!segment.getEventId().equals(eventId)) {
            throw new ApiException("SEGMENT_MISMATCH", "Segment does not belong to event");
        }
        if (segment.getUploadStatus() != UploadStatus.UPLOADED || segment.getStorageKey() == null) {
            throw new ApiException("NOT_UPLOADED", "Recording is not available yet");
        }
        return evidenceStorage.asResource(segment.getStorageKey());
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
        policeRepo.findNearest(lat, lng, 1).stream()
                .filter(station -> withinKm(lat, lng, station.getLatitude(), station.getLongitude(), 20))
                .findFirst()
                .ifPresent(station -> {
                    event.setPoliceStationId(station.getId());
                    audit(event.getId(), event.getUserId(), "POLICE_RESOLVED", Map.of(
                            "stationId", station.getId().toString(),
                            "verified", station.isPhoneVerified()
                    ));
                });
        eventRepo.save(event);
        audit(event.getId(), event.getUserId(), "LOCATION_UPDATED", Map.of("lat", lat, "lng", lng));
    }

    /**
     * Delivers an inactivity alert to one trusted contact (by priority order).
     * Reuses the emergency delivery pipeline with {@link TriggerType#INACTIVITY}.
     */
    @Transactional
    public UUID deliverInactivityContactAlert(
            UUID userId,
            UUID existingEventId,
            int priorityOrder,
            InactivityLevel level
    ) {
        List<ContactRole> roles = List.of(ContactRole.HELP_BACKUP);
        List<TrustedContactEntity> atPriority = contactRepo
                .findByOwnerUserIdAndContactRoleInAndActiveTrue(userId, roles)
                .stream()
                .filter(c -> c.getPriorityOrder() == priorityOrder)
                .toList();
        if (atPriority.isEmpty()) {
            atPriority = contactRepo.findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(userId)
                    .stream()
                    .filter(c -> c.getContactRole() == ContactRole.HELP_BACKUP)
                    .filter(c -> c.getPriorityOrder() == priorityOrder)
                    .toList();
        }
        if (atPriority.isEmpty()) {
            return existingEventId;
        }
        TrustedContactEntity contact = atPriority.stream()
                .filter(TrustedContactEntity::isVerified)
                .findFirst()
                .orElse(atPriority.get(0));

        EmergencyEventEntity event = null;
        if (existingEventId != null) {
            event = eventRepo.findById(existingEventId).orElse(null);
        }
        if (event == null) {
            event = EmergencyEventEntity.builder()
                    .userId(userId)
                    .triggerType(TriggerType.INACTIVITY)
                    .status(EmergencyStatus.TRIGGERED)
                    .mockDrill(false)
                    .build();
            event = eventRepo.save(event);
            audit(event.getId(), userId, "INACTIVITY_TRIGGERED", Map.of("level", level.name()));
            event.setStatus(EmergencyStatus.WAITING_SAFE);
            event = eventRepo.save(event);
        }

        UUID contactId = contact.getId();
        boolean already = deliveryRepo.findByEventId(event.getId()).stream()
                .anyMatch(d -> contactId.equals(d.getContactId()));
        if (already) {
            return event.getId();
        }

        ContactDeliveryEntity delivery = ContactDeliveryEntity.builder()
                .eventId(event.getId())
                .contactId(contact.getId())
                .contactName(contact.getName())
                .contactPhone(contact.getPhoneE164())
                .status(DeliveryStatus.PENDING)
                .build();
        delivery = deliveryRepo.save(delivery);

        UUID finalEventId = event.getId();
        UUID deliveryId = delivery.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.enqueueInactivityAlert(userId, finalEventId, deliveryId, level);
                }
            });
        } else {
            notificationService.enqueueInactivityAlert(userId, finalEventId, deliveryId, level);
        }
        return event.getId();
    }

    /** Re-queue FAILED/PENDING deliveries when user re-triggers while SOS is still open. */
    private void reNotifyFailedDeliveries(EmergencyEventEntity event) {
        List<ContactDeliveryEntity> deliveries = deliveryRepo.findByEventId(event.getId());
        int requeued = 0;
        for (ContactDeliveryEntity d : deliveries) {
            if (d.getStatus() == DeliveryStatus.FAILED
                    || d.getStatus() == DeliveryStatus.PENDING
                    || d.getStatus() == DeliveryStatus.UNKNOWN) {
                d.setStatus(DeliveryStatus.PENDING);
                d.setLastError(null);
                deliveryRepo.save(d);
                UUID userId = event.getUserId();
                UUID eventId = event.getId();
                UUID deliveryId = d.getId();
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
                requeued++;
            }
        }
        if (requeued > 0) {
            audit(event.getId(), event.getUserId(), "RESUME_RENNOTIFY", Map.of("requeued", requeued));
        } else if (deliveries.isEmpty()) {
            userRepo.findById(event.getUserId()).ifPresent(u -> notifyContacts(u, event));
        }
    }

    private void notifyContacts(UserEntity user, EmergencyEventEntity event) {
        // Both SOS ("can't take a call") and HELP ("call me urgent") alert SOS_TRUSTED only.
        // HELP_MONITOR / DOCTOR / NEIGHBOUR / HELP_BACKUP are pre-saved help numbers —
        // shown on the alert for coordination, never notified themselves.
        // INACTIVITY uses deliverInactivityContactAlert() instead of this path.
        List<ContactRole> roles = List.of(ContactRole.SOS_TRUSTED);
        List<TrustedContactEntity> contacts = contactRepo
                .findByOwnerUserIdAndContactRoleInAndActiveTrue(user.getId(), roles)
                .stream()
                .filter(TrustedContactEntity::isVerified)
                .toList();
        // Older contacts may never have been marked verified — still alert them
        // rather than silently dropping the SOS.
        if (contacts.isEmpty()) {
            contacts = contactRepo
                    .findByOwnerUserIdAndContactRoleInAndActiveTrue(user.getId(), roles);
            if (!contacts.isEmpty()) {
                audit(event.getId(), user.getId(), "ALERT_FALLBACK_UNVERIFIED_CONTACTS", Map.of(
                        "count", contacts.size(),
                        "trigger", event.getTriggerType().name()
                ));
            }
        }
        if (contacts.isEmpty()) {
            audit(event.getId(), user.getId(), "NO_VERIFIED_CONTACTS_FOR_ALERT", Map.of(
                    "trigger", event.getTriggerType().name()
            ));
            return;
        }
        // One delivery per unique phone — duplicate contacts must not fan out alerts.
        java.util.Set<String> phonesSeen = new java.util.LinkedHashSet<>();
        for (TrustedContactEntity c : contacts) {
            String phoneKey = com.pukaar.common.PhoneNumbers.digitsOnly(c.getPhoneE164());
            if (!phonesSeen.add(phoneKey)) {
                continue;
            }
            ContactDeliveryEntity delivery = ContactDeliveryEntity.builder()
                    .eventId(event.getId())
                    .contactId(c.getId())
                    .contactName(c.getName())
                    .contactPhone(com.pukaar.common.PhoneNumbers.toE164(c.getPhoneE164()))
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
        m.put("batteryPct", event.getBatteryPct());
        m.put("networkType", event.getNetworkType());
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
                sm.put("durationSec", s.getDurationSec());
                sm.put("byteSize", s.getByteSize());
                sm.put("contentType", s.getContentType());
                sm.put("uploadedAt", s.getUploadedAt());
                if (s.getUploadStatus() == UploadStatus.UPLOADED && s.getStorageKey() != null) {
                    sm.put("playUrl", "/api/v1/emergencies/" + event.getId() + "/audio-segments/" + s.getId() + "/content");
                }
                return sm;
            }).toList());
            if (event.getLatitude() != null && event.getLongitude() != null) {
                // Live Google Places / OSM first — seed DB is only a fallback
                try {
                    Map<String, Object> nearby = nearbyPlacesService.nearby(
                            event.getLatitude(), event.getLongitude(), 3);
                    firstPlace(nearby.get("police")).ifPresent(p -> m.put("policeStation", p));
                    firstPlace(nearby.get("hospitals")).ifPresent(h -> m.put("nearestHospital", h));
                    firstLiveAmbulance(nearby.get("ambulance")).ifPresent(a -> m.put("nearestAmbulance", a));
                    m.put("nearbySource", nearby.get("source"));
                } catch (Exception e) {
                    // fall through to DB
                }
                if (!m.containsKey("policeStation")) {
                    if (event.getPoliceStationId() != null) {
                        policeRepo.findById(event.getPoliceStationId())
                                .filter(p -> withinKm(event.getLatitude(), event.getLongitude(),
                                        p.getLatitude(), p.getLongitude(), 20))
                                .ifPresent(p -> m.put("policeStation", toPoliceDto(p)));
                    }
                    if (!m.containsKey("policeStation")) {
                        policeRepo.findNearest(event.getLatitude(), event.getLongitude(), 1).stream()
                                .filter(p -> withinKm(event.getLatitude(), event.getLongitude(),
                                        p.getLatitude(), p.getLongitude(), 20))
                                .findFirst()
                                .ifPresent(p -> m.put("policeStation", toPoliceDto(p)));
                    }
                }
                if (!m.containsKey("policeStation")) {
                    m.put("policeStation", Map.of(
                            "name", "Police Emergency",
                            "phone", "100",
                            "phoneVerified", true
                    ));
                }
                if (!m.containsKey("nearestHospital")) {
                    hospitalRepo.findNearest(event.getLatitude(), event.getLongitude(), 1).stream()
                            .filter(h -> withinKm(event.getLatitude(), event.getLongitude(),
                                    h.getLatitude(), h.getLongitude(), 20))
                            .findFirst()
                            .ifPresent(h -> m.put("nearestHospital", toHospitalDto(h)));
                }
                if (!m.containsKey("nearestHospital")) {
                    m.put("nearestHospital", Map.of(
                            "name", "Nearest Hospital / Emergency",
                            "phone", "112",
                            "phoneVerified", true
                    ));
                }
                if (!m.containsKey("nearestAmbulance")) {
                    m.put("nearestAmbulance", Map.of(
                            "name", "National Ambulance",
                            "phone", "108",
                            "source", "NATIONAL"
                    ));
                }
            } else if (event.getPoliceStationId() != null) {
                policeRepo.findById(event.getPoliceStationId()).ifPresent(p -> m.put("policeStation", toPoliceDto(p)));
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

    private Optional<Map<String, Object>> firstPlace(Object listObj) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return Optional.empty();
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> raw)) return Optional.empty();
        return Optional.of(sanitizePlace(raw));
    }

    /** Prefer a real nearby ambulance; skip static national 108/112 if a live place exists. */
    private Optional<Map<String, Object>> firstLiveAmbulance(Object listObj) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return Optional.empty();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) continue;
            Object source = raw.get("source");
            if ("NATIONAL".equals(String.valueOf(source))) continue;
            return Optional.of(sanitizePlace(raw));
        }
        return firstPlace(listObj);
    }

    /** Keep only fields the Android EmergencyDto adapters understand. */
    private Map<String, Object> sanitizePlace(Map<?, ?> raw) {
        Map<String, Object> m = new LinkedHashMap<>();
        copyIfPresent(raw, m, "name");
        copyIfPresent(raw, m, "phone");
        copyIfPresent(raw, m, "address");
        copyIfPresent(raw, m, "latitude");
        copyIfPresent(raw, m, "longitude");
        if (raw.get("phoneVerified") != null) m.put("phoneVerified", raw.get("phoneVerified"));
        else m.put("phoneVerified", raw.get("phone") != null);
        return m;
    }

    private void copyIfPresent(Map<?, ?> raw, Map<String, Object> out, String key) {
        Object v = raw.get(key);
        if (v != null) out.put(key, v);
    }

    private static boolean withinKm(double lat1, double lng1, Double lat2, Double lng2, double maxKm) {
        if (lat2 == null || lng2 == null) return false;
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double km = r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return km <= maxKm;
    }

    private Map<String, Object> toPoliceDto(PoliceStationEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", p.getName());
        m.put("address", p.getAddress());
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
        m.put("name", h.getName());
        m.put("phone", h.getPhoneE164());
        m.put("address", h.getAddress());
        m.put("latitude", h.getLatitude());
        m.put("longitude", h.getLongitude());
        m.put("phoneVerified", h.getPhoneE164() != null);
        return m;
    }
}
