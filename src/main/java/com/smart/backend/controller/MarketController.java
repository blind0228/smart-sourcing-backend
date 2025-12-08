package com.smart.backend.controller;

import com.smart.backend.dto.MarketAnalysisResponse;
import com.smart.backend.dto.RankingItem;
import com.smart.backend.service.MarketService;
import com.smart.backend.service.SourcingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
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

    // 2. POST /market/sourcing/request (분석 요청)
    @PostMapping("/sourcing/request")
    public ResponseEntity<Void> requestSourcing(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        sourcingService.sendAnalysisRequest(keyword);
        return ResponseEntity.accepted().build();
    }

    // 3. GET /market/ranking (랭킹 조회)
    @GetMapping("/ranking")
    public ResponseEntity<?> getRanking() {
        try {
            List<RankingItem> ranking = marketService.getNaverShoppingRanking();
            return ResponseEntity.ok(ranking != null ? ranking : List.of());
        } catch (Exception e) {
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

    // 5. POST /market/analysis (🔥 405 오류 해결: 워커 분석 결과 수신)
    // 워커가 분석 결과를 이 주소로 POST합니다.
    @PostMapping("/analysis")
    public ResponseEntity<Void> receiveAnalysisResult(@RequestBody MarketAnalysisResponse result) {
        log.info("🚀 Received analysis result for keyword: {}", result.getSearchKeyword());

        try {
            // Service 계층을 통해 DB에 저장합니다.
            marketService.saveAnalysisResult(result);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error saving analysis result: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 6. POST /market/ranking/receive (🔥 워커 초기 랭킹 수신)
    // 워커가 시작 시 랭킹 목록을 이 주소로 POST합니다.
    @PostMapping("/ranking/receive")
    public ResponseEntity<Void> receiveRankingList(@RequestBody List<RankingItem> rankingList) {
        log.info("📊 Received {} ranking items from worker.", rankingList.size());

        try {
            marketService.saveNaverRanking(rankingList);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error saving initial ranking list: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}