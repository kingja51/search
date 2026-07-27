package com.gonet.search.mapper;

import com.gonet.search.domain.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContentMapper {

    Content findById(@Param("id") Long id);
}
