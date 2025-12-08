// com.smart.backend.controller.MarketController.java (수정된 코드)

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
import java.util.stream.Collectors; // Collectors import 추가

@RestController
@RequestMapping({"/market", "/api/market"})
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    private final SourcingService sourcingService;

    // 1. GET /market/list (파라미터 없음, 수정 불필요)
    @GetMapping("/list")
    public ResponseEntity<List<MarketAnalysisResponse>> getAnalysisList() {
        List<MarketAnalysisResponse> list = marketService.findAllAnalysis();
        return ResponseEntity.ok(list);
    }

    // 2. POST /market/sourcing/request (React 요청 -> SQS 전송)
    // POST 요청 시 바디가 비어있으면 400이 발생하기 쉬움. @RequestParam에 required=true 기본값 사용
    @PostMapping("/sourcing/request")
    public ResponseEntity<Void> requestSourcing(@RequestParam String keyword) {
        // 이미 null/empty 체크가 있으므로 이 부분은 400을 직접 반환합니다.
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        sourcingService.sendAnalysisRequest(keyword);
        return ResponseEntity.accepted().build();
    }

    // 3. POST /market/analysis (Python Worker 분석 결과 수신)
    // ... (수정 불필요)

    // 4. POST /market/ranking/receive (Python Worker 랭킹 결과 수신 -> DB 저장)
    // ... (수정 불필요)

    // 5. GET /market/ranking (React 좌측 네이버 랭킹 표 - DB 조회)
    // 파라미터 없음, 수정 불필요
    @GetMapping("/ranking")
    public ResponseEntity<List<RankingItem>> getRanking() {
        List<RankingItem> ranking = marketService.getNaverShoppingRanking();
        return ResponseEntity.ok(ranking);
    }

    // 6. GET /market/ranking/category?categoryLabel=패션의류
    // 🔥 필수 파라미터를 옵션으로 변경 (400 오류 방지)
    // @RequestParam(required = false)를 사용하여 파라미터가 없어도 400 오류가 발생하지 않도록 합니다.
    @GetMapping("/ranking/category")
    public ResponseEntity<List<RankingItem>> getRankingByCategory(
            @RequestParam(required = false) String categoryLabel // ⭐ 변경: required = false 추가
    ) {
        // categoryLabel이 null이거나 비어있으면 전체 랭킹을 반환
        if (categoryLabel == null || categoryLabel.trim().isEmpty()) {
            // 400 Bad Request 대신, 전체 랭킹을 반환하거나 빈 리스트를 반환하여 오류를 피함
            List<RankingItem> allRanking = marketService.getNaverShoppingRanking();
            return ResponseEntity.ok(allRanking != null ? allRanking : List.of());
        }

        List<RankingItem> allRanking = marketService.getNaverShoppingRanking();

        if (allRanking == null || allRanking.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        String prefix = "[" + categoryLabel.trim() + "]";

        // keyword가 "[카테고리]"로 시작하는 것만 필터링해서 TOP 10 반환
        List<RankingItem> filtered = allRanking.stream()
                .filter(item -> item.getKeyword() != null && item.getKeyword().startsWith(prefix))
                .sorted(java.util.Comparator.comparingInt(RankingItem::getRank))
                .limit(10)
                .toList();

        // 카테고리 내에서 1위~10위로 다시 랭크 매김
        for (int i = 0; i < filtered.size(); i++) {
            filtered.get(i).setRank(i + 1);
        }

        return ResponseEntity.ok(filtered);
    }
}
