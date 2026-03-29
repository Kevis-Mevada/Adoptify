package com.adoptify.backend.service;

import com.adoptify.backend.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            logger.info("Upload directory initialized: {}", uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file) {
        return uploadFile(file, "general");
    }

    public String uploadFile(MultipartFile file, String subDirectory) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("File type not allowed. Allowed: " + ALLOWED_EXTENSIONS);
        }

        try {
            // Create subdirectory
            Path targetDir = Paths.get(uploadDir, subDirectory);
            Files.createDirectories(targetDir);

            // Generate unique filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String filename = timestamp + "_" + uniqueId + extension;

            // Save file
            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + subDirectory + "/" + filename;
            logger.info("File uploaded: {}", fileUrl);

            return fileUrl;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String relativePath = fileUrl.replace("/uploads/", "");
            Path filePath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(filePath);
            logger.info("File deleted: {}", fileUrl);
        } catch (IOException e) {
            logger.error("Failed to delete file {}: {}", fileUrl, e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg";
    }
}
