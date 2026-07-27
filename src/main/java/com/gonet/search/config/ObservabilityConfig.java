package com.gonet.search.config;

import com.gonet.search.mapper.SearchIndexMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;

/**
 * 관측성 구성 (DESIGN.md 6장).
 * - index.documents Gauge: 도메인별 색인 문서 수 (Prometheus 스크레이프 시 조회)
 * - TaskDecorator: @Async 스레드(검색 로그 적재)로 traceId 컨텍스트 전파
 * - search.query Timer 등 커스텀 메트릭은 각 서비스에서 기록
 */
@Configuration
public class ObservabilityConfig {

    /** @Async 실행 시 trace 컨텍스트 전파 — Boot가 TaskDecorator 빈을 기본 Executor에 자동 적용 */
    @Bean
    public TaskDecorator taskDecorator() {
        return new ContextPropagatingTaskDecorator();
    }

    @Bean
    public MeterBinder indexDocumentsGauge(SearchIndexMapper searchIndexMapper, SearchDocTypes docTypes) {
        return registry -> docTypes.codes().forEach(docType ->
                Gauge.builder("index.documents", () -> safeCount(searchIndexMapper, docType))
                        .tag("doc_type", docType)
                        .description("색인 문서 수")
                        .register(registry));
    }

    /** 스크레이프 시점에 DB가 내려가 있어도 메트릭 수집 전체가 실패하지 않도록 -1 반환 */
    private double safeCount(SearchIndexMapper mapper, String docType) {
        try {
            return mapper.count(docType);
        } catch (Exception e) {
            return -1;
        }
    }
}
