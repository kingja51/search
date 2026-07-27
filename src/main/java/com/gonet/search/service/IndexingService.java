package com.gonet.search.service;

import com.gonet.search.analyzer.KoreanAnalyzer;
import com.gonet.search.domain.SearchIndex;
import com.gonet.search.domain.SearchSource;
import com.gonet.search.mapper.SearchIndexMapper;
import com.gonet.search.mapper.SearchSourceMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 색인 동기화 파이프라인 (DESIGN.md 4.4).
 * 매일 2회 스케줄: vw_search_source ↔ tn_search_index 를 content_hash diff로 동기화.
 * 변경이 없으면 Nori 분석이 한 건도 실행되지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

    private final SearchSourceMapper searchSourceMapper;
    private final SearchIndexMapper searchIndexMapper;
    private final KoreanAnalyzer koreanAnalyzer;
    private final MeterRegistry meterRegistry;
    private final IndexJobLock jobLock;

    @Value("${search.index.chunk-size:500}")
    private int chunkSize;

    @Value("${search.result.summary-length:2000}")
    private int summaryLength;

    @Value("${search.index.sync-on-startup:false}")
    private boolean syncOnStartup;

    /** 마지막 동기화/재색인 결과 (어드민 화면 표시용) */
    @lombok.Getter
    private volatile SyncResult lastResult;

    /** 기동 직후 1회 동기화 (개발 편의: search.index.sync-on-startup) */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (syncOnStartup) {
            syncSearchIndex();
        }
    }

    /** 스케줄 동기화 — 기본 매일 06:00, 18:00 */
    @Scheduled(cron = "${search.index.sync-cron}")
    public void scheduledSync() {
        syncSearchIndex();
    }

    /** 해시 diff 동기화: 신규·변경 upsert + 삭제 반영. 다른 색인 작업 실행 중이면 null(건너뜀). */
    public SyncResult syncSearchIndex() {
        if (!jobLock.tryLock()) {
            log.warn("다른 색인 작업이 실행 중 — 동기화 건너뜀");
            return null;
        }
        try {
            long start = System.currentTimeMillis();
            List<SearchSource> changed = searchSourceMapper.findChanged();
            int upserted = upsertInChunks(changed);
            int deleted = searchIndexMapper.deleteOrphans();
            long elapsed = System.currentTimeMillis() - start;
            recordSyncTimer("diff", elapsed);
            log.info("색인 동기화 완료(diff): 신규·변경 {}건, 삭제 {}건, {}ms", upserted, deleted, elapsed);
            lastResult = new SyncResult("diff", upserted, deleted, elapsed);
            return lastResult;
        } finally {
            jobLock.unlock();
        }
    }

    /** 전체 재색인: 해시 비교 없이 전량 재분석 — 사전·품사 설정 변경 후 사용. 실행 중이면 null. */
    public SyncResult rebuildAll() {
        if (!jobLock.tryLock()) {
            log.warn("다른 색인 작업이 실행 중 — 전체 재색인 건너뜀");
            return null;
        }
        try {
            long start = System.currentTimeMillis();
            List<SearchSource> all = searchSourceMapper.findAll();
            int upserted = upsertInChunks(all);
            int deleted = searchIndexMapper.deleteOrphans();
            long elapsed = System.currentTimeMillis() - start;
            recordSyncTimer("full", elapsed);
            log.info("전체 재색인 완료(full): {}건, 삭제 {}건, {}ms", upserted, deleted, elapsed);
            lastResult = new SyncResult("full", upserted, deleted, elapsed);
            return lastResult;
        } finally {
            jobLock.unlock();
        }
    }

    private int upsertInChunks(List<SearchSource> sources) {
        int total = 0;
        for (int from = 0; from < sources.size(); from += chunkSize) {
            List<SearchIndex> items = sources.subList(from, Math.min(from + chunkSize, sources.size()))
                    .stream()
                    .map(this::toIndex)
                    .toList();
            searchIndexMapper.upsertBatch(items);
            total += items.size();
        }
        return total;
    }

    /**
     * 소스 1건 → 개인정보 마스킹 → Nori 분석 → 색인 행 변환.
     * 마스킹을 색인 시점에 수행하므로 tn_search_index(title·summary·tokens)에는
     * 개인정보가 저장되지 않고, 개인정보 숫자로 검색되지도 않는다.
     * ※ 마스킹 패턴 변경 시 전체 재색인 필요.
     */
    private SearchIndex toIndex(SearchSource source) {
        String title = com.gonet.search.util.MaskingUtil.mask(source.getTitle());
        String body = com.gonet.search.util.MaskingUtil.mask(
                source.getBody() == null ? "" : source.getBody());
        SearchIndex index = new SearchIndex();
        index.setDocType(source.getDocType());
        index.setDocId(source.getDocId());
        index.setTitle(title);
        index.setSummary(truncateSafely(body, summaryLength));
        index.setLinkUrl(sanitizeLinkUrl(source.getLinkUrl()));
        index.setCategory(source.getCategory());
        index.setTokens(String.join(" ", koreanAnalyzer.analyze(title + " " + body)));
        index.setContentHash(source.getContentHash());
        index.setSourceUpdatedAt(source.getUpdatedAt());
        return index;
    }

    /** 절단 경계가 서로게이트 페어(이모지 등)를 가르지 않게 요약을 자른다 */
    private String truncateSafely(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        int end = max;
        if (Character.isLowSurrogate(text.charAt(end))) {
            end--;
        }
        return text.substring(0, end);
    }

    /**
     * 검색 결과 링크 스킴 검증 — 상대 경로(/...)와 http/https만 허용.
     * 외부 소스를 색인하게 될 경우 javascript: 등 스킴 주입으로 href XSS가 되는 것을 차단한다.
     */
    private String sanitizeLinkUrl(String linkUrl) {
        if (linkUrl == null || linkUrl.isBlank()) {
            return "#";
        }
        String trimmed = linkUrl.strip();
        String lower = trimmed.toLowerCase();
        if (trimmed.startsWith("/") || lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }
        log.warn("허용되지 않는 link_url 스킴 — '#'로 대체: {}", trimmed);
        return "#";
    }

    private void recordSyncTimer(String mode, long elapsedMs) {
        Timer.builder("index.sync")
                .tag("mode", mode)
                .description("색인 동기화/전체 재색인 소요시간")
                .register(meterRegistry)
                .record(elapsedMs, TimeUnit.MILLISECONDS);
    }

    /** 동기화 결과 요약 (로그·어드민·메트릭용) */
    public record SyncResult(String mode, int upserted, int deleted, long elapsedMs) {
    }
}
