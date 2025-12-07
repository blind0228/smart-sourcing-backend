// com.smart.backend.repository.NaverRankingRepository.java
package com.smart.backend.repository;

import com.smart.backend.entity.NaverRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NaverRankingRepository extends JpaRepository<NaverRanking, Long> {

    // 최신 저장분을 ranking 오름차순으로 전부 조회
    List<NaverRanking> findAllByOrderByRankingAsc();

    // or TOP 30만 쓰고 싶으면
    // List<NaverRanking> findTop30ByOrderBySaveTimeDescRankingAsc();
}