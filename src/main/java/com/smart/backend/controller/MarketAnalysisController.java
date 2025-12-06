package com.smart.backend.controller;

import com.smart.backend.dto.MarketAnalysisRequest;
import com.smart.backend.entity.MarketAnalysis;
import com.smart.backend.repository.MarketAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market")
public class MarketAnalysisController {

    @Autowired
    private MarketAnalysisRepository repository;

    @PostMapping("/analysis")
    public ResponseEntity<String> saveAnalysis(@RequestBody MarketAnalysisRequest request) {

        System.out.println("📨 데이터 수신 중: " + request.getSearchKeyword());

        // DTO -> Entity 변환
        MarketAnalysis entity = new MarketAnalysis();
        entity.setSearchKeyword(request.getSearchKeyword());
        entity.setCategory(request.getCategory());
        entity.setAveragePrice(request.getAveragePrice());
        entity.setLowestPrice(request.getLowestPrice());
        entity.setSampleCount(request.getSampleCount());
        entity.setTopItemName(request.getTopItemName());

        // AWS RDS에 저장!
        repository.save(entity);

        System.out.println("✅ DB 저장 완료: " + entity.getId() + "번 데이터");

        return ResponseEntity.ok("Saved Successfully");
    }
}