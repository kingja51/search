package com.gonet.search.mapper;

import com.gonet.search.domain.DicBanned;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DicBannedMapper {

    /** 활성 금지어 전체 (금지어 필터 캐시 로딩용) */
    List<DicBanned> findAllEnabled();
}
