package com.gonet.search.service;

import com.gonet.search.analyzer.KoreanAnalyzer;
import com.gonet.search.domain.SearchIndex;
import com.gonet.search.domain.SearchSource;
import com.gonet.search.mapper.SearchIndexMapper;
import com.gonet.search.mapper.SearchSourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Value("${search.index.chunk-size:500}")
    private int chunkSize;

    @Value("${search.result.summary-length:2000}")
    private int summaryLength;

    @Value("${search.index.sync-on-startup:false}")
    private boolean syncOnStartup;

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

    /** 해시 diff 동기화: 신규·변경 upsert + 삭제 반영 */
    public synchronized SyncResult syncSearchIndex() {
        long start = System.currentTimeMillis();
        List<SearchSource> changed = searchSourceMapper.findChanged();
        int upserted = upsertInChunks(changed);
        int deleted = searchIndexMapper.deleteOrphans();
        long elapsed = System.currentTimeMillis() - start;
        log.info("색인 동기화 완료(diff): 신규·변경 {}건, 삭제 {}건, {}ms", upserted, deleted, elapsed);
        return new SyncResult("diff", upserted, deleted, elapsed);
    }

    /** 전체 재색인: 해시 비교 없이 전량 재분석 — 사전·품사 설정 변경 후 사용 */
    public synchronized SyncResult rebuildAll() {
        long start = System.currentTimeMillis();
        List<SearchSource> all = searchSourceMapper.findAll();
        int upserted = upsertInChunks(all);
        int deleted = searchIndexMapper.deleteOrphans();
        long elapsed = System.currentTimeMillis() - start;
        log.info("전체 재색인 완료(full): {}건, 삭제 {}건, {}ms", upserted, deleted, elapsed);
        return new SyncResult("full", upserted, deleted, elapsed);
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

    /** 소스 1건 → Nori 분석 → 색인 행 변환 */
    private SearchIndex toIndex(SearchSource source) {
        String body = source.getBody() == null ? "" : source.getBody();
        SearchIndex index = new SearchIndex();
        index.setDocType(source.getDocType());
        index.setDocId(source.getDocId());
        index.setTitle(source.getTitle());
        index.setSummary(body.length() > summaryLength ? body.substring(0, summaryLength) : body);
        index.setLinkUrl(source.getLinkUrl());
        index.setCategory(source.getCategory());
        index.setTokens(String.join(" ", koreanAnalyzer.analyze(source.getTitle() + " " + body)));
        index.setContentHash(source.getContentHash());
        index.setSourceUpdatedAt(source.getUpdatedAt());
        return index;
    }

    /** 동기화 결과 요약 (로그·어드민·메트릭용) */
    public record SyncResult(String mode, int upserted, int deleted, long elapsedMs) {
    }
}
