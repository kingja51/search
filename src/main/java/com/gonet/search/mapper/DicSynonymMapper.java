package com.gonet.search.mapper;

import com.gonet.search.domain.DicSynonym;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DicSynonymMapper {

    /** 활성 동의어 전체 (동의어 확장 캐시 로딩용) */
    List<DicSynonym> findAllEnabled();
}
