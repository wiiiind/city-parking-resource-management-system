package com.parking.system.dto;

import jakarta.validation.constraints.NotNull;

public record VehicleDeleteRequest(
        @NotNull(message = "车辆ID不能为空") Long vehicleId
) {
}
