package com.smart.backend.service;

import com.smart.backend.dto.MarketAnalysisResponse;
import com.smart.backend.dto.RankingItem;
import com.smart.backend.entity.MarketAnalysis;
import com.smart.backend.entity.NaverRanking;
import com.smart.backend.repository.MarketAnalysisRepository;
import com.smart.backend.repository.NaverRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketService {

    private final MarketAnalysisRepository analysisRepository;
    private final NaverRankingRepository rankingRepository;


    // React 상세 분석 목록 조회 (우측 표)
    public List<MarketAnalysisResponse> findAllAnalysis() {
        log.info("MarketService.findAllAnalysis 요청 처리");
        List<MarketAnalysisResponse> responses = analysisRepository.findAllByOrderByAnalysisDateDesc().stream()
                .map(MarketAnalysisResponse::from)
                .collect(Collectors.toList());
        log.debug("분석 목록 반환 크기: {}", responses.size());
        return responses;
    }

    // Python Worker 분석 결과 저장 (DTO -> Entity 변환 로직 추가 필요)
    @Transactional
    public void saveAnalysisResult(MarketAnalysisResponse dto) {
        // MarketAnalysisResponse DTO를 MarketAnalysis Entity로 변환하여 저장해야 합니다.
        // DTO에 AnalysisDate가 없다면 현재 시간을 사용합니다.
        MarketAnalysis entity = MarketAnalysis.from(dto);
        entity.setAnalysisDate(LocalDateTime.now());
        log.info("MarketService.saveAnalysisResult - 키워드: {}", dto.getSearchKeyword());
        analysisRepository.save(entity);
    }

    // Worker가 전송한 랭킹 결과를 DB에 저장
    @Transactional
    public void saveNaverRanking(List<RankingItem> rankingList) {

        // 🔥 한 방에 전체 삭제 (기존 데이터 클린)
        log.info("MarketService.saveNaverRanking - 기존 랭킹 삭제 및 새로운 {}건 저장 시작", rankingList.size());
        rankingRepository.deleteAllInBatch();

        LocalDateTime now = LocalDateTime.now();

        List<NaverRanking> entities = rankingList.stream()
                .map(item -> {
                    NaverRanking entity = new NaverRanking();
                    entity.setRanking(item.getRank());
                    entity.setKeyword(item.getKeyword());
                    entity.setSearchRatio(item.getSearchRatio());
                    entity.setSaveTime(now);
                    return entity;
                })
                .collect(Collectors.toList());

        rankingRepository.saveAll(entities);
        log.info("MarketService.saveNaverRanking - 저장 완료 ({}건)", entities.size());
    }

    // DB에서 랭킹 조회
    public List<RankingItem> getNaverShoppingRanking() {
        log.info("MarketService.getNaverShoppingRanking 호출");
        List<RankingItem> rankingItems = rankingRepository.findAllByOrderByRankingAsc()
                .stream()
                .map(e -> new RankingItem(
                        e.getRanking(),
                        e.getKeyword(),
                        e.getSearchRatio()
                ))
                .toList();
        log.debug("랭킹 조회 결과: {}건", rankingItems.size());
        return rankingItems;
    }
}
