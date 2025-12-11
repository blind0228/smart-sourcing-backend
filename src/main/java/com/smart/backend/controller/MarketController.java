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

    // 1. GET /market/list (분석 리스트 조회)
    @GetMapping("/list")
    public ResponseEntity<?> getAnalysisList() {
        // 리스트 조회는 너무 자주 찍히면 시끄러울 수 있으므로 디버그 레벨이나 간단하게 처리
        // 부하 테스트 중에는 이 로그가 너무 많다면 주석 처리 고려
        // log.info("🔍 [List 조회] 분석 리스트 요청");
        try {
            List<MarketAnalysisResponse> list = marketService.findAllAnalysis();
            int analysisSize = list != null ? list.size() : 0;
            // 리스트가 비어있을 때만 경고, 아니면 사이즈만 로깅
            if (analysisSize == 0) {
                log.info("⚠️ [List 조회] 현재 저장된 분석 결과가 없습니다.");
            }
            return ResponseEntity.ok(list != null ? list : List.of());
        } catch (Exception e) {
            log.error("❌ [List 조회 에러] ", e);
            return ResponseEntity.internalServerError().body("DB Error: " + e.getMessage());
        }
    }

    // 2. POST /market/sourcing/request (실제 운영 분석 요청)
    @PostMapping("/sourcing/request")
    public ResponseEntity<Void> requestSourcing(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("⚠️ [분석 요청 실패] 키워드가 비어있습니다 (POST)");
            return ResponseEntity.badRequest().build();
        }

        log.info("🟢 [분석 요청-POST] 키워드: '{}' -> SQS/Service 전송 시작", keyword);
        sourcingService.sendAnalysisRequest(keyword);

        return ResponseEntity.accepted().build();
    }

    // 2-T. GET /market/sourcing/test (부하 테스트용)
    @GetMapping("/sourcing/test")
    public ResponseEntity<Void> requestSourcingTest(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("⚠️ [TEST 요청 실패] 키워드가 비어있습니다 (GET)");
            return ResponseEntity.badRequest().build();
        }

        // Locust 테스트 시 로그가 폭주할 수 있으나, 요청이 제대로 들어오는지 확인이 필요함
        log.info("🧪 [TEST 분석 요청-GET] 키워드: '{}' -> 부하 테스트용 호출", keyword);

        sourcingService.sendAnalysisRequest(keyword);
        return ResponseEntity.accepted().build();
    }

    // 3. GET /market/ranking (랭킹 조회)
    @GetMapping("/ranking")
    public ResponseEntity<?> getRanking() {
        try {
            List<RankingItem> ranking = marketService.getNaverShoppingRanking();
            // (수정) 사용하지 않는 rankingSize 변수 삭제
            return ResponseEntity.ok(ranking != null ? ranking : List.of());
        } catch (Exception e) {
            log.error("❌ [랭킹 조회 에러] ", e);
            return ResponseEntity.internalServerError().body("DB Error: " + e.getMessage());
        }
    }

    // 4. GET /market/ranking/category
    @GetMapping("/ranking/category")
    public ResponseEntity<?> getRankingByCategory(@RequestParam(required = false) String categoryLabel) {
        // 상세 로그 요청 반영: 어떤 카테고리로 필터링하는지 확인
        log.info("📂 [카테고리 랭킹 조회] 카테고리 필터: {}", categoryLabel != null ? categoryLabel : "전체(ALL)");
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
            log.error("❌ [카테고리 랭킹 에러] ", e);
            return ResponseEntity.internalServerError().body("Server Error: " + e.getMessage());
        }
    }

    // 5. POST /market/analysis (워커 결과 수신 - 중요 로그)
    @PostMapping("/analysis")
    public ResponseEntity<Void> receiveAnalysisResult(@RequestBody MarketAnalysisResponse result) {
        String keyword = result.getSearchKeyword();

        // DTO에 존재하는 필드들로 로그 메시지를 재구성했습니다.
        String category = result.getCategory();
        int avgPrice = result.getAveragePrice();
        String competition = result.getCompetitionLevel();
        int score = result.getSourcingScore();
        String attractiveness = result.getMarketAttractiveness();

        log.info("📥 [워커 결과 수신] 키워드: '{}' | 카테고리: {} | 평균가: {}원 | 경쟁강도: {} | 소싱점수: {}점 ({})",
                keyword, category, avgPrice, competition, score, attractiveness);

        try {
            marketService.saveAnalysisResult(result);
            log.info("💾 [DB 저장 완료] 키워드: '{}' 분석 데이터 저장 성공", keyword);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ [결과 저장 실패] 키워드: '{}' - 에러: ", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 6. POST /market/ranking/receive (초기 랭킹 수신)
    @PostMapping("/ranking/receive")
    public ResponseEntity<Void> receiveRankingList(@RequestBody List<RankingItem> rankingList) {
        int size = (rankingList != null) ? rankingList.size() : 0;

        // (수정) rankingList != null 조건을 명시하여 IDE 경고 제거
        String topItem = (rankingList != null && !rankingList.isEmpty()) ? rankingList.get(0).getKeyword() : "없음";

        log.info("📊 [랭킹 업데이트 수신] 총 {}개 아이템 | 1위: {}", size, topItem);

        try {
            marketService.saveNaverRanking(rankingList);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ [랭킹 저장 실패] ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}