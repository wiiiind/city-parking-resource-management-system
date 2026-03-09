package com.parking.system.controller;

import com.parking.system.common.ApiResponse;
import com.parking.system.service.ParkingDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final ParkingDemoService parkingDemoService;

    public SystemController(ParkingDemoService parkingDemoService) {
        this.parkingDemoService = parkingDemoService;
    }

    @GetMapping("/notices")
    public ApiResponse<?> notices() {
        return ApiResponse.ok(parkingDemoService.listNotices());
    }

    @GetMapping("/logs")
    public ApiResponse<?> logs() {
        return ApiResponse.ok(parkingDemoService.listLogs());
    }
}
