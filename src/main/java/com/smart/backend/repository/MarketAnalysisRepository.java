package com.smart.backend.repository;
import com.smart.backend.entity.MarketAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface MarketAnalysisRepository extends JpaRepository<MarketAnalysis, Long> {
    List<MarketAnalysis> findAllByOrderByAnalysisDateDesc();
}