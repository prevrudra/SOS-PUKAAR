package com.pukaar.web;

import com.pukaar.common.ClosureReason;
import com.pukaar.common.TriggerType;
import com.pukaar.domain.emergency.EmergencyOrchestrator;
import com.pukaar.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergencies")
@RequiredArgsConstructor
public class EmergencyController {
    private final EmergencyOrchestrator orchestrator;

    @PostMapping("/trigger")
    public Map<String, Object> trigger(@RequestBody TriggerRequest req) {
        TriggerType type = req.getTriggerType() == null ? TriggerType.APP : req.getTriggerType();
        return orchestrator.trigger(
                SecurityUtils.currentUserId(),
                type,
                req.getLatitude(),
                req.getLongitude(),
                req.getAccuracyM(),
                Boolean.TRUE.equals(req.getMockDrill()),
                req.getBatteryPct(),
                req.getNetworkType()
        );
    }

    @GetMapping("/active")
    public Map<String, Object> active() {
        return orchestrator.getActive(SecurityUtils.currentUserId());
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        return orchestrator.getEvent(SecurityUtils.currentUserId(), id);
    }

    @PostMapping("/{id}/location")
    public Map<String, Object> location(@PathVariable UUID id, @RequestBody LocationRequest req) {
        return orchestrator.updateLocation(SecurityUtils.currentUserId(), id, req.getLatitude(), req.getLongitude(), req.getAccuracyM());
    }

    @PostMapping("/{id}/telemetry")
    public Map<String, Object> telemetry(@PathVariable UUID id, @RequestBody TelemetryRequest req) {
        return orchestrator.updateTelemetry(SecurityUtils.currentUserId(), id, req.getBatteryPct(), req.getNetworkType());
    }

    @PostMapping("/{id}/safe")
    public Map<String, Object> safe(@PathVariable UUID id, @RequestBody(required = false) SafeRequest req) {
        ClosureReason reason = req == null || req.getReason() == null ? ClosureReason.IM_SAFE : req.getReason();
        return orchestrator.markSafe(SecurityUtils.currentUserId(), id, reason);
    }

    @PostMapping("/{id}/audio-segments")
    public Map<String, Object> createSegment(@PathVariable UUID id, @RequestBody SegmentRequest req) {
        return orchestrator.registerAudioSegment(SecurityUtils.currentUserId(), id, req.getIndex(), req.getChecksumSha256(), req.getByteSize());
    }

    @PostMapping("/{id}/audio-segments/{segmentId}/uploaded")
    public Map<String, Object> uploaded(@PathVariable UUID id, @PathVariable UUID segmentId, @RequestBody UploadConfirmRequest req) {
        return orchestrator.markSegmentUploaded(SecurityUtils.currentUserId(), id, segmentId, req.getStorageKey());
    }

    @PostMapping(value = "/{id}/audio-segments/{segmentId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadSegment(
            @PathVariable UUID id,
            @PathVariable UUID segmentId,
            @RequestParam("file") MultipartFile file
    ) {
        return orchestrator.uploadAudioSegment(SecurityUtils.currentUserId(), id, segmentId, file);
    }

    @PostMapping("/mock-drills/{drillId}/complete")
    public Map<String, Object> completeDrill(@PathVariable UUID drillId, @RequestBody DrillCompleteRequest req) {
        return orchestrator.completeMockDrill(
                SecurityUtils.currentUserId(),
                drillId,
                Boolean.TRUE.equals(req.getContactsConfirmed()),
                req.getNotes()
        );
    }

    @PostMapping("/mock-drills/latest/complete")
    public Map<String, Object> completeLatestDrill(@RequestBody DrillCompleteRequest req) {
        return orchestrator.completeMockDrill(
                SecurityUtils.currentUserId(),
                null,
                Boolean.TRUE.equals(req.getContactsConfirmed()),
                req.getNotes()
        );
    }

    @Data
    public static class TriggerRequest {
        private TriggerType triggerType;
        private Double latitude;
        private Double longitude;
        private Double accuracyM;
        private Boolean mockDrill;
        private Integer batteryPct;
        private String networkType;
    }

    @Data
    public static class LocationRequest {
        private double latitude;
        private double longitude;
        private Double accuracyM;
    }

    @Data
    public static class TelemetryRequest {
        private Integer batteryPct;
        private String networkType;
    }

    @Data
    public static class SafeRequest {
        private ClosureReason reason;
    }

    @Data
    public static class SegmentRequest {
        private int index;
        private String checksumSha256;
        private Long byteSize;
    }

    @Data
    public static class UploadConfirmRequest {
        private String storageKey;
    }

    @Data
    public static class DrillCompleteRequest {
        private Boolean contactsConfirmed;
        private String notes;
    }
}
