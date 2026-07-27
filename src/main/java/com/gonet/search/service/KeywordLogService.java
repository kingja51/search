package com.gonet.search.service;

import com.gonet.search.domain.SearchKeywordLog;
import com.gonet.search.dto.KeywordStat;
import com.gonet.search.mapper.SearchKeywordLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 검색 키워드 로그 + 인기·내 검색어 (DESIGN.md 4.3 / 4.5).
 * 로그는 @Async 비동기 적재 — 검색 응답을 지연시키지 않는다.
 * 인기 검색어 MV는 10분 주기 자동 갱신(CONCURRENTLY, 조회 무중단).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordLogService {

    private final SearchKeywordLogMapper logMapper;

    /**
     * 검색 로그 비동기 적재.
     * 감사 필드(createdIp/createdBy)는 호출 측(요청 스레드)에서 캡처한 값을 세팅해 넘긴다
     * — @Async 스레드에서는 ClientIpHolder가 서버 IP로 폴백하기 때문 (AuditInterceptor는 null일 때만 채움)
     */
    @Async
    public void logAsync(SearchKeywordLog entry) {
        try {
            logMapper.insert(entry);
        } catch (Exception e) {
            log.warn("검색 로그 적재 실패: keyword={}", entry.getKeyword(), e);
        }
    }

    /** 인기 검색어 TOP N (vw_search_popular_keyword — 최근 7일, 차단 검색 제외) */
    public List<KeywordStat> popularKeywords(int limit) {
        return logMapper.findPopular(limit);
    }

    /** 내 검색어 — session_id 우선, 없으면 IP 폴백 (IP 파라미터 API 금지: 서버가 추출한 값만 사용) */
    public List<KeywordStat> myKeywords(String sessionId, String clientIp, int limit) {
        if (sessionId != null && !sessionId.isBlank()) {
            List<KeywordStat> bySession = logMapper.findRecentBySession(sessionId, limit);
            if (!bySession.isEmpty()) {
                return bySession;
            }
        }
        return logMapper.findRecentByIp(clientIp, limit);
    }

    /** 인기 검색어 자동 생성 — MV 10분 주기 갱신 (실패 시 다음 주기에 자동 재시도) */
    @Scheduled(cron = "${search.keyword.popular-refresh-cron}")
    public void refreshPopularKeywords() {
        try {
            long start = System.currentTimeMillis();
            logMapper.refreshPopular();
            log.info("인기 검색어 MV 갱신 완료 ({}ms)", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("인기 검색어 MV 갱신 실패 — 다음 주기에 재시도", e);
        }
    }
}
