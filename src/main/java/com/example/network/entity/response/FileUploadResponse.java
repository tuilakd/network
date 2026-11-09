package com.example.network.entity.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi sau khi upload file thành công.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private String fileUrl;
    private String fileType; // IMAGE hoặc AUDIO
    private String originalName;
    private long size;
}
