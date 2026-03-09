package com.parking.system.controller;

import com.parking.system.common.ApiResponse;
import com.parking.system.dto.LoginRequest;
import com.parking.system.service.ParkingDemoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ParkingDemoService parkingDemoService;

    public AuthController(ParkingDemoService parkingDemoService) {
        this.parkingDemoService = parkingDemoService;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", parkingDemoService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.ok(parkingDemoService.currentUser());
    }

    @GetMapping("/menus")
    public ApiResponse<?> menus(@RequestParam(defaultValue = "管理员") String role) {
        return ApiResponse.ok(parkingDemoService.menus(role));
    }
}
