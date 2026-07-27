package com.gonet.search.domain;

import com.gonet.search.config.ClientIpHolder;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * 공통 감사 컬럼 6종 (DESIGN.md 3.0 / 4.6).
 * 모든 tn_/log_ 테이블 엔티티는 반드시 이 클래스를 상속한다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;           // 생성일

    @Column(name = "created_ip", updatable = false, nullable = false, length = 45)
    private String createdIp;                   // 생성자 IP

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;                   // 생성자 ID

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;           // 수정일

    @Column(name = "updated_ip", length = 45)
    private String updatedIp;                   // 수정자 IP

    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;                   // 수정자 ID

    @PrePersist
    void fillCreatedIp() {
        this.createdIp = ClientIpHolder.get();
    }

    @PreUpdate
    void fillUpdatedIp() {
        this.updatedIp = ClientIpHolder.get();
    }
}
