package com.parking.system.dto;

import jakarta.validation.constraints.NotNull;

public record CheckOutRequest(
        @NotNull(message = "停车记录ID不能为空") Long recordId
) {
}
