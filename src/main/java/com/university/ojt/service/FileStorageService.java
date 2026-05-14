package com.university.ojt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    public String saveFile(MultipartFile file, String subfolder) {
        try {
            String filename = generateFilename(file.getOriginalFilename());
            Path directory = Paths.get(uploadDir, subfolder);
            Files.createDirectories(directory);
            Path targetPath = directory.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return subfolder + "/" + filename;
        } catch (IOException e) {
            logger.error("File upload failed: {}", e.getMessage());
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }

    public String saveBase64Photo(String base64Data, String subfolder, String prefix) {
        try {
            String[] parts = base64Data.split(",");
            byte[] imageBytes = java.util.Base64.getDecoder().decode(parts.length > 1 ? parts[1] : parts[0]);
            String filename = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
            Path directory = Paths.get(uploadDir, subfolder);
            Files.createDirectories(directory);
            Path targetPath = directory.resolve(filename);
            Files.write(targetPath, imageBytes);
            return subfolder + "/" + filename;
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Base64 photo save failed: {}", e.getMessage());
            throw new RuntimeException("Failed to save photo: " + e.getMessage());
        }
    }

    private String generateFilename(String original) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 6);
        String ext = getExtension(original != null ? original : "file.jpg");
        return timestamp + "_" + uuid + "." + ext;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "jpg";
    }
}
