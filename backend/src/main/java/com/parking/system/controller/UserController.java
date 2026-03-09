package com.parking.system.controller;

import com.parking.system.common.ApiResponse;
import com.parking.system.dto.VehicleCreateRequest;
import com.parking.system.service.ParkingDemoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ParkingDemoService parkingDemoService;

    public UserController(ParkingDemoService parkingDemoService) {
        this.parkingDemoService = parkingDemoService;
    }

    @GetMapping
    public ApiResponse<?> users() {
        return ApiResponse.ok(parkingDemoService.listUsers());
    }

    @GetMapping("/vehicles")
    public ApiResponse<?> vehicles() {
        return ApiResponse.ok(parkingDemoService.listVehicles());
    }

    @PostMapping("/vehicles")
    public ApiResponse<?> createVehicle(@Valid @RequestBody VehicleCreateRequest request) {
        return ApiResponse.ok("车辆新增成功", parkingDemoService.createVehicle(request));
    }
}
