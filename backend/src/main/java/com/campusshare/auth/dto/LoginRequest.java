package com.campusshare.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "账号不能为空")
        @Size(max = 64, message = "账号长度不能超过64个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 72, message = "密码长度不能超过72个字符")
        String password
) {
}
