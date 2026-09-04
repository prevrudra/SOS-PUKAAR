package com.pukaar.domain.evidence;

import com.pukaar.common.ApiException;
import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EvidenceStorageService {
    private final PukaarProperties props;

    public String store(UUID eventId, UUID segmentId, MultipartFile file) throws IOException {
        Path base = basePath();
        Files.createDirectories(base);
        Path dir = base.resolve(eventId.toString());
        Files.createDirectories(dir);
        String filename = segmentId + ".m4a";
        Path dest = dir.resolve(filename);
        file.transferTo(dest);
        return eventId + "/" + filename;
    }

    public Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("..")) {
            throw new ApiException("INVALID_STORAGE_KEY", "Invalid evidence path");
        }
        Path base = basePath().toAbsolutePath().normalize();
        Path resolved = base.resolve(storageKey).normalize();
        if (!resolved.startsWith(base)) {
            throw new ApiException("INVALID_STORAGE_KEY", "Invalid evidence path");
        }
        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new ApiException("EVIDENCE_MISSING", "Recording file not found on server");
        }
        return resolved;
    }

    public Resource asResource(String storageKey) {
        return new FileSystemResource(resolve(storageKey));
    }

    private Path basePath() {
        return Paths.get(props.getStorage().getLocalPath());
    }
}
