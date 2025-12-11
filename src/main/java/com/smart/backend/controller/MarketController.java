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
@RequestMapping({"/market", "/api/market"})
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    private final SourcingService sourcingService;

    // 1. GET /market/list
    @GetMapping("/list")
    public ResponseEntity<?> getAnalysisList() {
        log.info("GET /market/list 요청 처리 시작");
        try {
            List<MarketAnalysisResponse> list = marketService.findAllAnalysis();
            int analysisSize = list != null ? list.size() : 0;
            log.info("GET /market/list 응답 - 분석 건수: {}", analysisSize);
            return ResponseEntity.ok(list != null ? list : List.of());
        } catch (Exception e) {
            log.error("Error in /market/list: ", e);
            return ResponseEntity.internalServerError().body("DB Error: " + e.getMessage());
        }
    }

    // 2. POST /market/sourcing/request (운영 분석 요청) - 원본 유지
    @PostMapping("/sourcing/request")
    public ResponseEntity<Void> requestSourcing(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("POST /market/sourcing/request - 키워드: {}", keyword);
        sourcingService.sendAnalysisRequest(keyword);
        return ResponseEntity.accepted().build();
    }

    // ⭐⭐ 새로 추가된 테스트용 API ⭐⭐
    // 2-T. GET /market/sourcing/test (부하 테스트 전용 분석 요청)
    // Locust에서 405 에러 없이 POST 대신 GET 요청으로 부하를 주기 위해 사용합니다.
    @GetMapping("/sourcing/test")
    public ResponseEntity<Void> requestSourcingTest(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        // 운영 API와 동일한 SourcingService 로직을 호출합니다.
        log.info("GET /market/sourcing/test - 부하 테스트 요청: {}", keyword);
        sourcingService.sendAnalysisRequest(keyword);
        return ResponseEntity.accepted().build();
    }
    // ⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐

    // 3. GET /market/ranking (랭킹 조회)
    @GetMapping("/ranking")
    public ResponseEntity<?> getRanking() {
        log.info("GET /market/ranking 요청 처리 시작");
        try {
            List<RankingItem> ranking = marketService.getNaverShoppingRanking();
            int rankingSize = ranking != null ? ranking.size() : 0;
            log.info("GET /market/ranking 응답 - 랭킹 항목 수: {}", rankingSize);
            return ResponseEntity.ok(ranking != null ? ranking : List.of());
        } catch (Exception e) {
            log.error("Error in /market/ranking: ", e);
            return ResponseEntity.internalServerError().body("DB Error: " + e.getMessage());
        }
    }

    // 4. GET /market/ranking/category
    @GetMapping("/ranking/category")
    public ResponseEntity<?> getRankingByCategory(@RequestParam(required = false) String categoryLabel) {
        log.info("GET /market/ranking/category 요청 - categoryLabel={}", categoryLabel);
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

    // 5. POST /market/analysis (워커 분석 결과 수신)
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

    // 6. POST /market/ranking/receive (워커 초기 랭킹 수신)
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