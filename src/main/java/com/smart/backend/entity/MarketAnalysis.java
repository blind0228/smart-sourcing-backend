package com.smart.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// ... (imports)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "market_analysis")
public class MarketAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String searchKeyword;
    private String category; private int averagePrice; private int lowestPrice;
    @Column(length = 500) private String topItemName; private int sampleCount;
    // 고도화 및 통합 지표
    private int totalListings; private String competitionLevel;
    private int searchVolumeRatio; private String marketAttractiveness;
    private int sourcingScore; // ⬅️ 통합 점수
    @Column(updatable = false) private LocalDateTime analysisDate = LocalDateTime.now();
}