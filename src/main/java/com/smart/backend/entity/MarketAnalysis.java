package com.smart.backend.entity;

import com.smart.backend.dto.MarketAnalysisResponse; // DTO 변환을 위해 반드시 필요합니다.
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "market_analysis")
public class MarketAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    // 기본 지표
    @Column(nullable = false) private String searchKeyword;
    private String category;
    private int averagePrice;
    private int lowestPrice;
    @Column(length = 500) private String topItemName;
    private int sampleCount;

    // 고도화 및 통합 지표
    private int totalListings;
    private String competitionLevel;
    private int searchVolumeRatio;
    private String marketAttractiveness;
    private int sourcingScore; // ⬅️ 통합 점수

    @Column(updatable = false) private LocalDateTime analysisDate = LocalDateTime.now();


    // ⭐⭐⭐ MarketService의 컴파일 오류를 해결하는 핵심 정적 메서드 ⭐⭐⭐
    // DTO -> Entity 변환 (저장용)
    public static MarketAnalysis from(MarketAnalysisResponse dto) {
        MarketAnalysis entity = new MarketAnalysis();

        // DTO 필드를 Entity로 매핑합니다.
        entity.setSearchKeyword(dto.getSearchKeyword());
        entity.setCategory(dto.getCategory());
        entity.setAveragePrice(dto.getAveragePrice());
        entity.setLowestPrice(dto.getLowestPrice());
        entity.setTopItemName(dto.getTopItemName());
        entity.setSampleCount(dto.getSampleCount());

        // 고도화 지표 매핑
        entity.setTotalListings(dto.getTotalListings());
        entity.setCompetitionLevel(dto.getCompetitionLevel());
        entity.setSearchVolumeRatio(dto.getSearchVolumeRatio());
        entity.setMarketAttractiveness(dto.getMarketAttractiveness());
        entity.setSourcingScore(dto.getSourcingScore());

        // analysisDate는 MarketService에서 LocalDateTime.now()로 별도 설정합니다.

        return entity;
    }
}