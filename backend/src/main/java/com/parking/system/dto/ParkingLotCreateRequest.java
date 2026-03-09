package com.parking.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParkingLotCreateRequest(
        @NotBlank(message = "停车场名称不能为空") String name,
        @NotBlank(message = "停车场编码不能为空") String code,
        @NotBlank(message = "地址不能为空") String address,
        @NotNull(message = "总车位不能为空") @Min(value = 1, message = "总车位必须大于0") Integer totalSpaces,
        @NotBlank(message = "营业时间不能为空") String businessHours
) {
}
