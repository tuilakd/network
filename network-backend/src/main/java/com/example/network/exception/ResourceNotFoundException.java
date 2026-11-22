package com.example.network.exception;

import lombok.NonNull;

import java.util.Arrays;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(@NonNull NetError... netErrors) {
        super(Arrays.toString(netErrors));
    }
}