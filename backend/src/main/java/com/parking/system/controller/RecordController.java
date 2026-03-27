package com.parking.system.controller;

import com.parking.system.common.ApiResponse;
import com.parking.system.dto.CheckInRequest;
import com.parking.system.dto.CheckOutRequest;
import com.parking.system.dto.RecordUpdateRequest;
import com.parking.system.service.ParkingDemoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final ParkingDemoService parkingDemoService;

    public RecordController(ParkingDemoService parkingDemoService) {
        this.parkingDemoService = parkingDemoService;
    }

    @GetMapping
    public ApiResponse<?> records() {
        return ApiResponse.ok(parkingDemoService.listRecords());
    }

    @GetMapping("/orders")
    public ApiResponse<?> orders() {
        return ApiResponse.ok(parkingDemoService.listOrders());
    }

    @PostMapping("/check-in")
    public ApiResponse<?> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ApiResponse.ok("车辆入场成功", parkingDemoService.checkIn(request));
    }

    @PostMapping("/check-out")
    public ApiResponse<?> checkOut(@Valid @RequestBody CheckOutRequest request) {
        return ApiResponse.ok("车辆出场成功", parkingDemoService.checkOut(request.recordId()));
    }

    @PostMapping("/orders/pay")
    public ApiResponse<?> payOrder(@Valid @RequestBody CheckOutRequest request) {
        return ApiResponse.ok("订单支付成功", parkingDemoService.payOrder(request.recordId()));
    }

    @PutMapping("/{recordId}")
    public ApiResponse<?> updateRecord(@PathVariable Long recordId, @Valid @RequestBody RecordUpdateRequest request) {
        return ApiResponse.ok("停车记录更新成功", parkingDemoService.updateRecord(recordId, request));
    }
}
