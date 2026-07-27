package com.gonet.search.repository;

import com.gonet.search.domain.RecommendKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RecommendKeywordRepository extends JpaRepository<RecommendKeyword, Long> {

    /** 노출 대상 추천 검색어: 활성 + 노출기간(NULL=상시) 충족, 순서대로 */
    @Query("""
            SELECT r FROM RecommendKeyword r
            WHERE r.enabled = true
              AND (r.startDate IS NULL OR r.startDate <= :today)
              AND (r.endDate   IS NULL OR r.endDate   >= :today)
            ORDER BY r.displayOrder ASC, r.id ASC
            """)
    List<RecommendKeyword> findDisplayable(@Param("today") LocalDate today);
}
