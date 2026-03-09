package com.parking.system.controller;

import com.parking.system.common.ApiResponse;
import com.parking.system.dto.ParkingLotCreateRequest;
import com.parking.system.service.ParkingDemoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingDemoService parkingDemoService;

    public ParkingController(ParkingDemoService parkingDemoService) {
        this.parkingDemoService = parkingDemoService;
    }

    @GetMapping("/lots")
    public ApiResponse<?> parkingLots() {
        return ApiResponse.ok(parkingDemoService.listParkingLots());
    }

    @PostMapping("/lots")
    public ApiResponse<?> createParkingLot(@Valid @RequestBody ParkingLotCreateRequest request) {
        return ApiResponse.ok("停车场新增成功", parkingDemoService.createParkingLot(request));
    }

    @GetMapping("/regions")
    public ApiResponse<?> regions() {
        return ApiResponse.ok(parkingDemoService.listRegions());
    }

    @GetMapping("/spaces")
    public ApiResponse<?> spaces() {
        return ApiResponse.ok(parkingDemoService.listSpaces());
    }

    @GetMapping("/pricing-rules")
    public ApiResponse<?> pricingRules() {
        return ApiResponse.ok(parkingDemoService.listPricingRules());
    }
}
