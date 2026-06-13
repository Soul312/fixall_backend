package com.fixall.backend.service;

import com.fixall.backend.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded files on the local filesystem.
 * Files are saved under {@code uploads/<subDir>/} with a UUID-based filename.
 * Returns a relative URL path that can be served via the static resource config.
 */
@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif",
        "application/pdf"
    );

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Store a file in a sub-directory.
     *
     * @param file   the uploaded multipart file
     * @param subDir sub-directory name (e.g. "id-docs", "certifications", "job-photos")
     * @return the relative URL path, e.g. "/uploads/id-docs/abc123.jpg"
     */
    public String storeFile(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(
                "Unsupported file type: " + contentType +
                ". Allowed: " + ALLOWED_CONTENT_TYPES);
        }

        // Max 20MB (also enforced by Spring, but double-check)
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new BadRequestException("File size exceeds 20MB limit");
        }

        return write(file, subDir);
    }

    /**
     * Store an image file (jpeg/png/webp/gif) with a custom size limit.
     *
     * @param maxBytes maximum allowed size in bytes
     * @return the relative URL path, e.g. "/uploads/job-photos/abc.jpg"
     */
    public String storeImage(MultipartFile file, String subDir, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed (got: " + contentType + ")");
        }

        if (file.getSize() > maxBytes) {
            throw new BadRequestException("Image exceeds the " + (maxBytes / (1024 * 1024)) + "MB limit");
        }

        return write(file, subDir);
    }

    /** Writes a validated file to disk and returns its relative URL path. */
    private String write(MultipartFile file, String subDir) {
        try {
            Path dirPath = Paths.get(uploadDir, subDir);
            Files.createDirectories(dirPath);

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf('.'));
            }
            String storedName = UUID.randomUUID() + extension;

            Path targetPath = dirPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = "/uploads/" + subDir + "/" + storedName;
            log.info("Stored file: {} -> {}", originalName, relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }

    /**
     * Delete a previously stored file by its relative URL path.
     */
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            // Strip leading "/uploads/" to get the filesystem path
            String fsPath = relativePath.replaceFirst("^/uploads/", "");
            Path filePath = Paths.get(uploadDir, fsPath);
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath, e);
        }
    }
}
