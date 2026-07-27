package com.gonet.search.mapper;

import com.gonet.search.domain.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileMapper {

    File findById(@Param("id") Long id);

    /** 텍스트 추출 대상: 최근 N개월 + 선언 확장자 + 경로 존재 (ACTIVE) */
    java.util.List<File> findExtractTargets(@Param("fromTs") java.time.OffsetDateTime fromTs,
                                            @Param("extensions") java.util.Collection<String> extensions);
}
