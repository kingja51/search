package com.gonet.search.mapper;

import com.gonet.search.domain.DicSynonym;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DicSynonymMapper {

    /** 활성 동의어 전체 (동의어 확장 캐시 로딩용) */
    List<DicSynonym> findAllEnabled();

    /** 전체 목록 (어드민 — 비활성 포함, 그룹 순) */
    List<DicSynonym> findAll();

    int insert(DicSynonym synonym);

    int toggleEnabled(@Param("id") Long id, @Param("ip") String ip);

    int deleteById(@Param("id") Long id);
}
