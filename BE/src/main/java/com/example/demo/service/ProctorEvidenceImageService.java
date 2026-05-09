package com.example.demo.service;

import com.example.demo.common.ApiException;
import com.example.demo.common.VietNamTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProctorEvidenceImageService {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final Path storageRoot;
    private final String publicUrlPrefix;

    public ProctorEvidenceImageService(
            @Value("${app.proctor.evidence-image-dir:${user.dir}/data/proctor-evidence}") String storageDir,
            @Value("${app.proctor.evidence-image-url-prefix:/evidence-images}") String publicUrlPrefix
    ) {
        this.storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
        this.publicUrlPrefix = normalizePrefix(publicUrlPrefix);
    }

    public StoredEvidenceImage storeFrameImage(
            Long attemptId,
            String frameId,
            String signalType,
            String imageBase64
    ) {
        if (attemptId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing attempt id");
        }
        byte[] imageBytes = decodeImage(imageBase64);
        ImageFormat format = detectFormat(imageBase64, imageBytes);
        String safeFrameId = safeSegment(frameId, "frame");
        String safeSignalType = safeSegment(signalType, "signal");
        String fileName = FILE_TIMESTAMP.format(VietNamTime.now())
                + "-" + safeFrameId
                + "-" + safeSignalType
                + "-" + UUID.randomUUID().toString().substring(0, 8)
                + format.extension;

        Path attemptDir = storageRoot.resolve("attempt-" + attemptId).normalize();
        Path target = attemptDir.resolve(fileName).normalize();
        if (!target.startsWith(attemptDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid evidence image path");
        }

        try {
            Files.createDirectories(attemptDir);
            Files.write(target, imageBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu ảnh bằng chứng");
        }

        String imageUrl = publicUrlPrefix + "/attempt-" + attemptId + "/" + fileName;
        LocalDateTime storedAt = VietNamTime.now();
        return new StoredEvidenceImage(
                imageUrl,
                target.toAbsolutePath().toString(),
                fileName,
                format.contentType,
                imageBytes.length,
                storedAt
        );
    }

    public LoadedEvidenceImage loadEvidenceImage(Long attemptId, String fileName) {
        if (attemptId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing attempt id");
        }
        String safeFileName = safeSegment(fileName, "");
        if (safeFileName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing evidence image name");
        }

        Path attemptDir = storageRoot.resolve("attempt-" + attemptId).normalize();
        Path target = attemptDir.resolve(safeFileName).normalize();
        if (!target.startsWith(attemptDir)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid evidence image path");
        }
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Evidence image not found");
        }

        String contentType = detectContentType(safeFileName, target);
        try {
            Resource resource = new UrlResource(target.toUri());
            return new LoadedEvidenceImage(resource, contentType, safeFileName);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Evidence image not found");
        }
    }

    private byte[] decodeImage(String imageBase64) {
        if (imageBase64 == null || imageBase64.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing camera frame");
        }
        String raw = imageBase64.trim();
        int comma = raw.indexOf(',');
        if (raw.startsWith("data:") && comma > 0) {
            raw = raw.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid camera frame payload");
        }
    }

    private ImageFormat detectFormat(String imageBase64, byte[] bytes) {
        String declaredType = null;
        if (imageBase64 != null && imageBase64.startsWith("data:")) {
            int comma = imageBase64.indexOf(',');
            if (comma > 5) {
                String header = imageBase64.substring(5, comma);
                int semi = header.indexOf(';');
                declaredType = (semi > 0 ? header.substring(0, semi) : header).trim().toLowerCase(Locale.ROOT);
            }
        }
        String contentType = normalizeContentType(declaredType);
        if (contentType == null) {
            contentType = sniffContentType(bytes);
        }
        if (contentType == null) {
            contentType = "image/jpeg";
        }
        return new ImageFormat(contentType, extensionForContentType(contentType));
    }

    private String detectContentType(String fileName, Path target) {
        String contentType = extensionToContentType(extensionOf(fileName));
        if (contentType != null) {
            return contentType;
        }
        try {
            String probe = Files.probeContentType(target);
            String normalized = normalizeContentType(probe);
            return normalized != null ? normalized : "image/jpeg";
        } catch (IOException ex) {
            return "image/jpeg";
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "image/jpeg", "image/jpg", "image/png", "image/webp" -> normalized.equals("image/jpg") ? "image/jpeg" : normalized;
            default -> null;
        };
    }

    private String sniffContentType(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return null;
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return "image/png";
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private String extensionForContentType(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private String extensionToContentType(String extension) {
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> null;
        };
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private String normalizePrefix(String prefix) {
        String normalized = prefix == null || prefix.isBlank() ? "/evidence-images" : prefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String safeSegment(String value, String fallback) {
        String raw = value == null ? "" : value.trim();
        String normalized = raw.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^\\.+", "")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
        if (normalized.isBlank()) {
            normalized = fallback == null ? "" : fallback;
        }
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }
        return normalized;
    }

    public record StoredEvidenceImage(
            String imageUrl,
            String storagePath,
            String fileName,
            String contentType,
            long sizeBytes,
            LocalDateTime storedAt
    ) {
    }

    public record LoadedEvidenceImage(
            Resource resource,
            String contentType,
            String fileName
    ) {
    }

    private record ImageFormat(String contentType, String extension) {
    }
}
