package com.example.network.service.impl;

import com.example.network.entity.response.FileUploadResponse;
import com.example.network.exception.BadRequestException;
import com.example.network.exception.ResourceNotFoundException;
import com.example.network.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Path uploadLocation;

    public FileStorageServiceImpl(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.uploadLocation);
            log.info("Thư mục lưu trữ file: {}", this.uploadLocation);
        } catch (IOException e) {
            log.error("Không thể khởi tạo thư mục uploads: {}", e.getMessage());
            throw new RuntimeException("Không thể khởi tạo thư mục lưu trữ file", e);
        }
    }

    @Override
    public FileUploadResponse storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Dung lượng file vượt quá giới hạn 10MB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "file";
        }
        originalName = originalName.replace("\\", "/");
        if (originalName.contains("/")) {
            originalName = originalName.substring(originalName.lastIndexOf('/') + 1);
        }

        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalName.substring(dotIndex).toLowerCase();
        }

        String fileType = determineFileType(ext);
        if (fileType == null) {
            throw new BadRequestException("Loại file không được hỗ trợ. Chỉ chấp nhận ảnh (jpg, png, gif, webp) hoặc ghi âm (mp3, wav, ogg, m4a, webm)");
        }

        String uniqueFileName = UUID.randomUUID().toString() + ext;
        try {
            Path targetLocation = this.uploadLocation.resolve(uniqueFileName).normalize();
            if (!targetLocation.startsWith(this.uploadLocation)) {
                throw new BadRequestException("Tên file không hợp lệ");
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Lưu file thành công: {} ({} bytes, loại: {})", uniqueFileName, file.getSize(), fileType);

            String fileUrl = "/api/files/" + uniqueFileName;
            return new FileUploadResponse(fileUrl, fileType, originalName, file.getSize());
        } catch (IOException e) {
            log.error("Lỗi khi ghi file vào ổ đĩa: {}", e.getMessage());
            throw new RuntimeException("Không thể lưu file trên máy chủ", e);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new BadRequestException("Tên file không hợp lệ");
            }
            Path filePath = this.uploadLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.uploadLocation)) {
                throw new BadRequestException("Tên file không hợp lệ");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Không tìm thấy file: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Không tìm thấy file: " + fileName);
        }
    }

    @Override
    public String determineContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".m4a")) return "audio/x-m4a";
        if (lower.endsWith(".webm")) return "audio/webm";
        return "application/octet-stream";
    }
    @Override
    public boolean validateAttachment(String fileUrl, String fileType) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return false;
        }
        String prefix = "/api/files/";
        if (!fileUrl.startsWith(prefix)) {
            throw new BadRequestException("Đường dẫn file đính kèm không hợp lệ");
        }
        String fileName = fileUrl.substring(prefix.length());
        loadFileAsResource(fileName); // kiểm tra tên file an toàn và file có thật trên server

        int dotIndex = fileName.lastIndexOf('.');
        String ext = (dotIndex >= 0) ? fileName.substring(dotIndex).toLowerCase() : "";
        String actualType = determineFileType(ext);
        if (actualType == null || !actualType.equals(fileType)) {
            throw new BadRequestException("Loại file đính kèm không khớp với file đã upload");
        }
        return true;
    }
    private String determineFileType(String ext) {
        if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".gif") || ext.equals(".webp")) {
            return "IMAGE";
        }
        if (ext.equals(".mp3") || ext.equals(".wav") || ext.equals(".ogg") || ext.equals(".m4a") || ext.equals(".webm")) {
            return "AUDIO";
        }
        return null;
    }
}
