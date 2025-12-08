package com.smart.backend.controller;

import com.smart.backend.dto.MarketAnalysisResponse;
import com.smart.backend.dto.RankingItem;
import com.smart.backend.service.MarketService;
import com.smart.backend.service.SourcingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로그를 위해 추가 권장
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j // 롬복 로그 추가
@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    private final SourcingService sourcingService;

    // 1. GET /market/list
    @GetMapping("/list")
    public ResponseEntity<?> getAnalysisList() {
        try {
            List<MarketAnalysisResponse> list = marketService.findAllAnalysis();
            return ResponseEntity.ok(list != null ? list : List.of());
        } catch (Exception e) {
            log.error("Error in /market/list: ", e);
            return ResponseEntity.internalServerError().body("DB Error: " + e.getMessage());
        }
    }

    // 2. POST /market/sourcing/request
    @PostMapping("/sourcing/request")
    public ResponseEntity<Void> requestSourcing(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        sourcingService.sendAnalysisRequest(keyword);
        return ResponseEntity.accepted().build();
    }

    // 3. GET /market/ranking (여기가 404 뜨던 곳)
    @GetMapping("/ranking")
    public ResponseEntity<?> getRanking() {
        try {
            List<RankingItem> ranking = marketService.getNaverShoppingRanking();
            return ResponseEntity.ok(ranking != null ? ranking : List.of());
        } catch (Exception e) {
            // 혹시 DB 에러가 나면 404가 아니라 500이 뜨도록 하여 원인 파악
            log.error("Error in /market/ranking: ", e);
            return ResponseEntity.internalServerError().body("DB Error: " + e.getMessage());
        }
    }

    // 4. GET /market/ranking/category
    @GetMapping("/ranking/category")
    public ResponseEntity<?> getRankingByCategory(@RequestParam(required = false) String categoryLabel) {
        try {
            List<RankingItem> allRanking = marketService.getNaverShoppingRanking();

            if (allRanking == null || allRanking.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            if (categoryLabel == null || categoryLabel.trim().isEmpty()) {
                return ResponseEntity.ok(allRanking);
            }

            String prefix = "[" + categoryLabel.trim() + "]";
            List<RankingItem> filtered = allRanking.stream()
                    .filter(item -> item.getKeyword() != null && item.getKeyword().startsWith(prefix))
                    .sorted(java.util.Comparator.comparingInt(RankingItem::getRank))
                    .limit(10)
                    .toList();

            for (int i = 0; i < filtered.size(); i++) {
                filtered.get(i).setRank(i + 1);
            }
            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            log.error("Error in /market/ranking/category: ", e);
            return ResponseEntity.internalServerError().body("Server Error: " + e.getMessage());
        }
    }
}