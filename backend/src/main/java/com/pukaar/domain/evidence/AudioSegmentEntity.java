package com.pukaar.domain.evidence;

import com.pukaar.common.UploadStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audio_evidence_segments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AudioSegmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    @Column(name = "segment_index", nullable = false)
    private int segmentIndex;
    @Column(name = "duration_sec", nullable = false)
    @Builder.Default
    private int durationSec = 60;
    @Column(name = "storage_key")
    private String storageKey;
    @Column(name = "content_type")
    @Builder.Default
    private String contentType = "audio/mp4";
    @Column(name = "byte_size")
    private Long byteSize;
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;
    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 20)
    @Builder.Default
    private UploadStatus uploadStatus = UploadStatus.PENDING;
    @Column(name = "uploaded_at")
    private Instant uploadedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
