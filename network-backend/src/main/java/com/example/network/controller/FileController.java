package com.example.network.controller;

import com.example.network.entity.response.FileUploadResponse;
import com.example.network.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller xử lý upload và tải/xem file đính kèm (ảnh, ghi âm).
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                         Authentication auth) {
        log.info("Người dùng '{}' upload file: {} ({} bytes)", auth.getName(), file.getOriginalFilename(), file.getSize());
        FileUploadResponse response = fileStorageService.storeFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        String contentType = fileStorageService.determineContentType(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
