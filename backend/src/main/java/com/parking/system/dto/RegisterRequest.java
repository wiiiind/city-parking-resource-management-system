package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        @NotBlank(message = "姓名不能为空") String realName,
        @NotBlank(message = "手机号不能为空") String phone
) {
}
