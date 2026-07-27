package com.gonet.search.mapper;

import com.gonet.search.domain.Bbs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BbsMapper {

    Bbs findById(@Param("id") Long id);

    List<Bbs> findByBoardCd(@Param("boardCd") String boardCd);
}
