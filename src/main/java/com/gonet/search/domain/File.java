package com.gonet.search.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 첨부파일 메타 + 본문 추출 텍스트 (검색 대상 원본) */
@Entity
@Table(name = "tn_file")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 300)
    private String fileName;

    @Column(name = "file_ext", length = 20)
    private String fileExt;                     // pdf, hwp, docx ...

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "extract_text", columnDefinition = "text")
    private String extractText;                 // 파일 본문 추출 텍스트 (색인 대상)

    @Column(name = "ref_type", length = 20)
    private String refType;                     // 첨부 출처 (CONTENT/BBS 등)

    @Column(name = "ref_id")
    private Long refId;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}
