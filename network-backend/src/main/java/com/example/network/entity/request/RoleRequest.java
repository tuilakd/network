package com.example.network.entity.request;

import com.example.network.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu cập nhật quyền người dùng (USER hoặc ADMIN).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    @NotNull(message = "Role không được để trống")
    private UserRole role;
}
