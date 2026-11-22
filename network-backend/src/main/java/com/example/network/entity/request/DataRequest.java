package com.example.network.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String content;
}