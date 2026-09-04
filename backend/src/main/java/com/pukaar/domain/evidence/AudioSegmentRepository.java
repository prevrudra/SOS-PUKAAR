package com.pukaar.domain.evidence;

import com.pukaar.common.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AudioSegmentRepository extends JpaRepository<AudioSegmentEntity, UUID> {
    List<AudioSegmentEntity> findByEventIdOrderBySegmentIndexAsc(UUID eventId);
    List<AudioSegmentEntity> findByUploadStatusIn(List<UploadStatus> statuses);
}
