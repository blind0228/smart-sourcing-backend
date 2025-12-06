package com.smart.backend.repository;

import com.smart.backend.entity.MarketAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketAnalysisRepository extends JpaRepository<MarketAnalysis, Long> {
}