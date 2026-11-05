package com.example.network.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleRequest {

    @NotBlank
    @Pattern(regexp = "USER|ADMIN", message = "Role phải là USER hoặc ADMIN")
    private String role;
}