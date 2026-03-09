package com.parking.system.controller;

import com.parking.system.common.ApiResponse;
import com.parking.system.service.ParkingDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ParkingDemoService parkingDemoService;

    public DashboardController(ParkingDemoService parkingDemoService) {
        this.parkingDemoService = parkingDemoService;
    }

    @GetMapping("/overview")
    public ApiResponse<?> overview() {
        return ApiResponse.ok(parkingDemoService.dashboardOverview());
    }

    @GetMapping("/dispatch-suggestions")
    public ApiResponse<?> dispatchSuggestions() {
        return ApiResponse.ok(parkingDemoService.dispatchSuggestions());
    }
}
