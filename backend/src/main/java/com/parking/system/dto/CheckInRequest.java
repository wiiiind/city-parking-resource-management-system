package com.parking.system.dto;

import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
        @NotNull(message = "车辆ID不能为空") Long vehicleId,
        @NotNull(message = "停车场ID不能为空") Long parkingLotId
) {
}
