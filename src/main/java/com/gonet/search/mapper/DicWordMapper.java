package com.gonet.search.mapper;

import com.gonet.search.domain.DicWord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DicWordMapper {

    /** 활성 단어사전 전체 (Nori 사용자 사전 로딩용) */
    List<DicWord> findAllEnabled();
}
