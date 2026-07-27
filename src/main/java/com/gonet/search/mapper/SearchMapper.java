package com.gonet.search.mapper;

import com.gonet.search.dto.KeyCount;
import com.gonet.search.dto.SearchResultItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** FTS 검색 쿼리 (DESIGN.md 4.3) — tsquery는 SearchService가 조합한 완성식 */
@Mapper
public interface SearchMapper {

    /** 개별 탭 검색: 카테고리·기간 필터 + 정렬(accuracy/latest) 분기 + 페이징 */
    List<SearchResultItem> searchTab(@Param("tsquery") String tsquery,
                                     @Param("docType") String docType,
                                     @Param("category") String category,
                                     @Param("fromTs") OffsetDateTime fromTs,
                                     @Param("toTs") OffsetDateTime toTs,
                                     @Param("sort") String sort,
                                     @Param("size") int size,
                                     @Param("offset") int offset);

    /** 전체 탭 그룹 검색: 도메인별 상위 N건 + 그룹 총건수 (row_number 윈도우, 쿼리 1회) */
    List<SearchResultItem> searchGrouped(@Param("tsquery") String tsquery,
                                         @Param("fromTs") OffsetDateTime fromTs,
                                         @Param("toTs") OffsetDateTime toTs,
                                         @Param("sort") String sort,
                                         @Param("groupSize") int groupSize);

    /** 탭별 건수 (전체 탭 헤더 "전체 124 · 컨텐츠 80 …") */
    List<KeyCount> countByType(@Param("tsquery") String tsquery,
                               @Param("fromTs") OffsetDateTime fromTs,
                               @Param("toTs") OffsetDateTime toTs);

    /** 개별 탭 내 카테고리별 건수 (카테고리 필터 UI) */
    List<KeyCount> countByCategory(@Param("tsquery") String tsquery,
                                   @Param("docType") String docType,
                                   @Param("fromTs") OffsetDateTime fromTs,
                                   @Param("toTs") OffsetDateTime toTs);

    /** 자동완성: 색인 제목 유사도(pg_trgm) 추천 */
    List<String> autocomplete(@Param("q") String q, @Param("limit") int limit);
}
