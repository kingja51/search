package com.gonet.search.mapper;

import com.gonet.search.domain.DicWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DicWordMapper {

    /** 활성 단어사전 전체 (Nori 사용자 사전 로딩용) */
    List<DicWord> findAllEnabled();

    /** 전체 목록 (어드민 — 비활성 포함) */
    List<DicWord> findAll();

    int insert(DicWord word);

    int toggleEnabled(@Param("id") Long id, @Param("ip") String ip);

    int deleteById(@Param("id") Long id);
}
