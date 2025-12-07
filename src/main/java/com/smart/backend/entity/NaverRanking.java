package com.smart.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

// com.smart.backend.entity.NaverRanking.java
@Entity
@Table(name = "naver_ranking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NaverRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DB 컬럼: ranking
    private int ranking;

    private String keyword;

    private long searchRatio;

    private LocalDateTime saveTime;
}
