package com.example.network.entity.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequest {

    @Size(max = 2000)
    private String content;

    private String attachmentUrl;

    private String attachmentType;
}