package com.gonet.search.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 공통 감사 필드 6종 (DESIGN.md 3.0 / 4.6).
 * 모든 tn_/log_ 테이블 도메인 클래스는 반드시 이 클래스를 상속한다.
 * 값 주입은 MyBatis AuditInterceptor가 INSERT/UPDATE 시점에 자동 수행한다.
 */
@Getter
@Setter
public abstract class BaseEntity {

    private OffsetDateTime createdAt;   // 생성일
    private String createdIp;           // 생성자 IP
    private String createdBy;           // 생성자 ID
    private OffsetDateTime updatedAt;   // 수정일
    private String updatedIp;           // 수정자 IP
    private String updatedBy;           // 수정자 ID
}
