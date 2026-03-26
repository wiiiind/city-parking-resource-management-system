package com.parking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecordUpdateRequest(
        @NotBlank(message = "入场时间不能为空") String entryTime,
        String exitTime,
        @NotNull(message = "费用不能为空") BigDecimal amount,
        @NotBlank(message = "支付状态不能为空") String paymentStatus
) {
}
