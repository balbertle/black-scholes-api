package com.example.quant.controller;

import com.example.quant.dto.HeatmapRequest;
import com.example.quant.dto.HeatmapResponse;
import com.example.quant.service.BlackScholesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/black-scholes")
public class BlackScholesController {

    private final BlackScholesService blackScholesService;

    @Autowired
    public BlackScholesController(BlackScholesService blackScholesService) {
        this.blackScholesService = blackScholesService;
    }

    @PostMapping("/heatmap")
    public ResponseEntity<HeatmapResponse> getHeatmapData(@RequestBody HeatmapRequest request) {
        try {
            HeatmapResponse response = blackScholesService.generateHeatmapData(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
