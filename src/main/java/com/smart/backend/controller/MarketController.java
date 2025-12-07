// com.smart.backend.controller.MarketController.java (전체 코드)

package com.smart.backend.controller;

import com.smart.backend.dto.MarketAnalysisResponse;
import com.smart.backend.dto.RankingItem;
import com.smart.backend.entity.MarketAnalysis;
import com.smart.backend.service.MarketService;
import com.smart.backend.service.SourcingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    private final SourcingService sourcingService;

    // 1. GET /api/market/list (React 우측 상세 분석 표)
    @GetMapping("/list")
    public ResponseEntity<List<MarketAnalysisResponse>> getAnalysisList() {
        List<MarketAnalysisResponse> list = marketService.findAllAnalysis();
        return ResponseEntity.ok(list);
    }

    // 2. POST /api/sourcing/request (React 요청 -> SQS 전송)
    @PostMapping("/sourcing/request")
    public ResponseEntity<Void> requestSourcing(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        sourcingService.sendAnalysisRequest(keyword);
        return ResponseEntity.accepted().build();
    }

    // 3. POST /api/market/analysis (Python Worker 분석 결과 수신)
    @PostMapping("/analysis")
    public ResponseEntity<Void> receiveAnalysisResult(@RequestBody MarketAnalysis analysisResult) {
        if (analysisResult.getSearchKeyword() == null) {
            return ResponseEntity.badRequest().build();
        }
        marketService.saveAnalysisResult(analysisResult);
        return ResponseEntity.ok().build();
    }

    // 4. POST /api/market/ranking/receive (Python Worker 랭킹 결과 수신 -> DB 저장)
    @PostMapping("/ranking/receive")
    public ResponseEntity<Void> receiveRankingResult(@RequestBody List<RankingItem> rankingList) {
        if (rankingList == null || rankingList.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        marketService.saveNaverRanking(rankingList);
        return ResponseEntity.ok().build();
    }

    // 5. GET /api/market/ranking (React 좌측 네이버 랭킹 표 - DB 조회)
    @GetMapping("/ranking")
    public ResponseEntity<List<RankingItem>> getRanking() {
        List<RankingItem> ranking = marketService.getNaverShoppingRanking();
        return ResponseEntity.ok(ranking);
    }

    // 6. GET /api/market/ranking/category?categoryLabel=패션의류
    @GetMapping("/ranking/category")
    public ResponseEntity<List<RankingItem>> getRankingByCategory(
            @RequestParam String categoryLabel
    ) {
        // DB에서 전체 랭킹 가져오기 (기존 서비스 재사용)
        List<RankingItem> allRanking = marketService.getNaverShoppingRanking();

        if (allRanking == null || allRanking.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        String prefix = "[" + categoryLabel + "]";  // 예: "[패션의류]"

        // keyword가 "[카테고리]"로 시작하는 것만 필터링해서 TOP 10 반환
        List<RankingItem> filtered = allRanking.stream()
                .filter(item -> item.getKeyword() != null && item.getKeyword().startsWith(prefix))
                // 필요하면 정렬 기준 변경 가능 (지금은 rank 기준 오름차순)
                .sorted(java.util.Comparator.comparingInt(RankingItem::getRank))
                .limit(10)
                .toList();

        // 카테고리 내에서 1위~10위로 다시 랭크 매기고 싶으면 아래처럼 재설정
        for (int i = 0; i < filtered.size(); i++) {
            filtered.get(i).setRank(i + 1);
        }

        return ResponseEntity.ok(filtered);
    }
}