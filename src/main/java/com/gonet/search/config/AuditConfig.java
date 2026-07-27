package com.gonet.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

/**
 * JPA Auditing 활성화. 생성자/수정자 ID 공급.
 * 로그인 도입 전: 웹 요청 = guest, 배치·스케줄러 = system.
 * Spring Security 도입 시 이 빈만 인증 사용자 ID 반환으로 교체한다. (DESIGN.md 4.6)
 */
@Configuration
@EnableJpaAuditing
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(
                RequestContextHolder.getRequestAttributes() != null ? "guest" : "system");
    }
}
