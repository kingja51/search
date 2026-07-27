package com.gonet.search.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 첨부파일 메타 + 본문 추출 텍스트 (검색 대상 원본, tn_file) */
@Getter
@Setter
@NoArgsConstructor
public class File extends BaseEntity {

    private Long id;
    private String fileName;
    private String fileExt;                     // pdf, hwp, docx ...
    private Long fileSize;
    private String filePath;
    private String extractText;                 // 파일 본문 추출 텍스트 (색인 대상)
    private String refType;                     // 첨부 출처 (CONTENT/BBS 등)
    private Long refId;
    private String status = "ACTIVE";
}
