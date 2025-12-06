package com.smart.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "market_analysis") // DB에 생성될 테이블 이름
public class MarketAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String searchKeyword; // 검색어 (예: 손난로)
    private String category;      // 카테고리

    private int averagePrice;     // 평균가
    private int lowestPrice;      // 최저가
    private int sampleCount;      // 샘플 개수 (예: 100개)

    @Column(length = 500)         // 상품명이 길 수도 있으니 넉넉하게
    private String topItemName;   // 1등 상품명

    private LocalDateTime createdAt; // 생성 시간

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}