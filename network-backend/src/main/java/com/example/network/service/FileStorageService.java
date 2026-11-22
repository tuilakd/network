package com.example.network.service;

import com.example.network.entity.response.FileUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadResponse storeFile(MultipartFile file);

    Resource loadFileAsResource(String fileName);

    String determineContentType(String fileName);

    boolean validateAttachment(String fileUrl, String fileType);
}
