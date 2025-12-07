// com.smart.backend.dto.MarketAnalysisResponse.java

package com.smart.backend.dto;

import com.smart.backend.entity.MarketAnalysis;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketAnalysisResponse {
    private Long id;
    private String searchKeyword;
    private String category;
    private int averagePrice;
    private int lowestPrice;
    private String topItemName;

    // ⬇️ 고도화 분석 지표 추가
    private int totalListings;
    private String competitionLevel;
    private int searchVolumeRatio;
    private String marketAttractiveness;
    private int sourcingScore;
    private LocalDateTime analysisDate;

    public static MarketAnalysisResponse from(MarketAnalysis entity) {
        return MarketAnalysisResponse.builder()
                .id(entity.getId())
                .searchKeyword(entity.getSearchKeyword())
                .category(entity.getCategory())
                .averagePrice(entity.getAveragePrice())
                .lowestPrice(entity.getLowestPrice())
                .topItemName(entity.getTopItemName())
                .totalListings(entity.getTotalListings())
                .competitionLevel(entity.getCompetitionLevel())
                .searchVolumeRatio(entity.getSearchVolumeRatio())
                .marketAttractiveness(entity.getMarketAttractiveness())
                .analysisDate(entity.getAnalysisDate())
                .sourcingScore(entity.getSourcingScore())
                .build();
    }
}