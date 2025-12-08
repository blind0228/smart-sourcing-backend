// com.smart.backend.controller.MarketController.java (최종 수정 버전)

package com.smart.backend.controller;

import com.smart.backend.dto.MarketAnalysisResponse;
import com.smart.backend.dto.RankingItem;
import com.smart.backend.service.MarketService;
import com.smart.backend.service.SourcingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional; // Optional 임포트 추가 (필요 시)

@RestController
@RequestMapping("/market") // ✅ 경로 단순화: ALB가 이미 /api 접두사를 제거하므로 /market만 유지
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    private final SourcingService sourcingService;

    // --- 1. GET /market/list (마켓 분석 목록 조회) ---
    // 파라미터 없음. 수정 불필요.
    @GetMapping("/list")
    public ResponseEntity<List<MarketAnalysisResponse>> getAnalysisList() {
        List<MarketAnalysisResponse> list = marketService.findAllAnalysis();
        // NullPointerException 방지를 위해 null 체크를 추가합니다.
        return ResponseEntity.ok(list != null ? list : List.of());
    }

    // --- 2. POST /market/sourcing/request (React 요청 -> SQS 전송) ---
    // keyword 파라미터가 필수이므로, 없으면 400을 반환하도록 로직 유지
    @PostMapping("/sourcing/request")
    public ResponseEntity<Void> requestSourcing(@RequestParam String keyword) {
        // @RequestParam의 기본값은 required=true이며, keyword가 없으면 Spring이 400을 반환합니다.
        // 따라서 명시적인 null/empty 체크는 없어도 되지만, 사용자님의 기존 로직을 유지합니다.
        if (keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        sourcingService.sendAnalysisRequest(keyword);
        return ResponseEntity.accepted().build();
    }

    // --- 3. POST /market/analysis (Python Worker 분석 결과 수신) ---
    // ... (필요한 경우 여기에 추가)

    // --- 4. POST /market/ranking/receive (Python Worker 랭킹 결과 수신 -> DB 저장) ---
    // ... (필요한 경우 여기에 추가)

    // --- 5. GET /market/ranking (React 좌측 네이버 랭킹 표 - DB 조회) ---
    // 파라미터 없음. 수정 불필요.
    @GetMapping("/ranking")
    public ResponseEntity<List<RankingItem>> getRanking() {
        List<RankingItem> ranking = marketService.getNaverShoppingRanking();
        return ResponseEntity.ok(ranking != null ? ranking : List.of());
    }

    // --- 6. GET /market/ranking/category?categoryLabel=패션의류 (카테고리별 랭킹) ---
    // ✅ 파라미터가 없어도 400 오류가 발생하지 않도록 required = false 설정 유지
    @GetMapping("/ranking/category")
    public ResponseEntity<List<RankingItem>> getRankingByCategory(
            @RequestParam(required = false) String categoryLabel // ⭐ 400 오류 방지 설정
    ) {
        List<RankingItem> allRanking = marketService.getNaverShoppingRanking();

        if (allRanking == null || allRanking.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // categoryLabel이 없으면 전체 랭킹을 반환
        if (categoryLabel == null || categoryLabel.trim().isEmpty()) {
            return ResponseEntity.ok(allRanking);
        }

        String prefix = "[" + categoryLabel.trim() + "]";

        // keyword가 "[카테고리]"로 시작하는 것만 필터링
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