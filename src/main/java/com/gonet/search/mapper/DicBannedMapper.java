package com.gonet.search.mapper;

import com.gonet.search.domain.DicBanned;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DicBannedMapper {

    /** 활성 금지어 전체 (금지어 필터 캐시 로딩용) */
    List<DicBanned> findAllEnabled();

    /** 전체 목록 (어드민 — 비활성 포함) */
    List<DicBanned> findAll();

    int insert(DicBanned banned);

    int toggleEnabled(@Param("id") Long id, @Param("ip") String ip);

    int deleteById(@Param("id") Long id);
}
