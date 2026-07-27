package com.gonet.search.mapper;

import com.gonet.search.domain.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileMapper {

    File findById(@Param("id") Long id);

    /** 텍스트 추출 대상: 최근 파일 + 선언 확장자 + 경로 존재 (ACTIVE). 비교용 extract_text 포함 */
    java.util.List<File> findExtractTargets(@Param("fromTs") java.time.OffsetDateTime fromTs,
                                            @Param("extensions") java.util.Collection<String> extensions);

    /** 추출 본문(마스킹 완료본) 반영 — content_hash가 바뀌어 diff 동기화가 색인을 갱신한다 */
    int updateExtractText(File file);
}
