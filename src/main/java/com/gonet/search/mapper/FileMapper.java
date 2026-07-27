package com.gonet.search.mapper;

import com.gonet.search.domain.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileMapper {

    File findById(@Param("id") Long id);
}
