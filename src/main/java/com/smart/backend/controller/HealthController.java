package com.smart.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String checkHealth() {
        return "Smart Seller Backend is Running! (v1.0)";
    }
}