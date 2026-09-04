package com.pukaar.domain.evidence;

import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
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
        Path base = Paths.get(props.getStorage().getLocalPath());
        Files.createDirectories(base);
        Path dir = base.resolve(eventId.toString());
        Files.createDirectories(dir);
        String filename = segmentId + ".m4a";
        Path dest = dir.resolve(filename);
        file.transferTo(dest);
        return eventId + "/" + filename;
    }
}
