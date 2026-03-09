package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleCreateRequest(
        @NotNull(message = "用户ID不能为空") Long userId,
        @NotBlank(message = "车牌号不能为空") String plateNumber,
        @NotBlank(message = "车辆品牌不能为空") String brand,
        @NotBlank(message = "车辆颜色不能为空") String color
) {
}
