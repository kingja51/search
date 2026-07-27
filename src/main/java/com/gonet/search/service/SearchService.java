package com.gonet.search.service;

import com.gonet.search.analyzer.KoreanAnalyzer;
import com.gonet.search.config.ClientIpHolder;
import com.gonet.search.domain.SearchKeywordLog;
import com.gonet.search.dto.KeyCount;
import com.gonet.search.dto.SearchCondition;
import com.gonet.search.dto.SearchGroup;
import com.gonet.search.dto.SearchResponse;
import com.gonet.search.dto.SearchResultItem;
import com.gonet.search.mapper.SearchMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 검색 오케스트레이션 (DESIGN.md 4.3).
 * 정규화 → 금지어 → 형태소 분석 → 동의어 확장 → tsquery(AND/OR·qPrev) → FTS → 하이라이트 → 로그(@Async)
 * 관측성: 전 과정 search.query Timer + 단계별 span(search.analyze/expand/query/highlight) (DESIGN.md 6장)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private static final int MAX_QUERY_LENGTH = 300;

    private final KoreanAnalyzer koreanAnalyzer;
    private final BannedWordService bannedWordService;
    private final SynonymService synonymService;
    private final HighlightService highlightService;
    private final KeywordLogService keywordLogService;
    private final SearchMapper searchMapper;
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;

    @Value("${search.result.group-order}")
    private String groupOrderConfig;

    @Value("${search.result.group-size:10}")
    private int groupSize;

    public SearchResponse search(SearchCondition cond, String sessionId) {
        long start = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);
        String clientIp = ClientIpHolder.get();          // @Async 로그용으로 요청 스레드에서 캡처
        String q = normalize(cond.getQ());
        String docType = cond.isAllTab() ? null : cond.getType().toUpperCase();
        if (q.isEmpty()) {
            return SearchResponse.empty("", "검색어를 입력해 주세요.");
        }

        // 1) 금지어 검사 — qPrev 포함 전체 검색어를 매 요청 재검사 (URL 조작 우회 방지)
        BannedWordService.Snapshot banned = bannedWordService.snapshot();
        List<String> prevQueries = cond.getQPrev().stream()
                .map(this::normalize).filter(s -> !s.isEmpty()).toList();
        for (String query : concat(prevQueries, q)) {
            Optional<String> hit = banned.findBlocked(query);
            if (hit.isPresent()) {
                meterRegistry.counter("search.blocked").increment();
                stopQueryTimer(sample, docType, true);
                logSearch(q, null, null, cond, 0, true, sessionId, clientIp, start);
                return SearchResponse.blocked(q);
            }
        }

        // 2) 형태소 분석 (span: search.analyze) — qPrev 포함 검색어별 토큰화 + MASK 제외
        Map<String, List<String>> tokensByQuery = observe("search.analyze", () -> {
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (String query : concat(prevQueries, q)) {
                map.put(query, koreanAnalyzer.analyze(query).stream()
                        .distinct()
                        .filter(t -> !banned.isMasked(t))
                        .toList());
            }
            return map;
        });
        if (tokensByQuery.get(q).isEmpty()) {
            stopQueryTimer(sample, docType, false);
            logSearch(q, "", "", cond, 0, false, sessionId, clientIp, start);
            return SearchResponse.empty(q, "검색 가능한 단어가 없습니다. 다른 검색어로 시도해 주세요.");
        }

        // 3) 동의어 확장 + tsquery 조합 (span: search.expand)
        //    그룹 내 항상 OR, 그룹 간 op(AND/OR), qPrev는 항상 AND 결합
        Set<String> highlightTerms = new LinkedHashSet<>();
        String tsquery = observe("search.expand", () -> {
            Map<String, Set<String>> synonyms = synonymService.expansionMap();
            List<String> exprs = new ArrayList<>();
            for (String prev : prevQueries) {
                String expr = buildExpr(tokensByQuery.get(prev), "AND", banned, synonyms, highlightTerms);
                if (expr != null) {
                    exprs.add(expr);
                }
            }
            exprs.add(buildExpr(tokensByQuery.get(q), cond.getOp(), banned, synonyms, highlightTerms));
            return exprs.stream().map(e -> "(" + e + ")").collect(Collectors.joining(" & "));
        });

        // 4) 기간 계산 (dateFrom/dateTo가 period보다 우선)
        OffsetDateTime fromTs = resolveFromTs(cond);
        OffsetDateTime toTs = resolveToTs(cond);

        // 5) FTS 실행 (span: search.query)
        SearchResponse res = new SearchResponse();
        res.setQ(q);
        res.setPage(cond.getPage());
        res.setSize(cond.getSize());
        observe("search.query", () -> {
            List<KeyCount> typeCounts = searchMapper.countByType(tsquery, fromTs, toTs);
            long grandTotal = typeCounts.stream().mapToLong(KeyCount::getCount).sum();
            res.getTabCounts().put("ALL", grandTotal);
            typeCounts.forEach(c -> res.getTabCounts().put(c.getKey(), c.getCount()));

            if (cond.isAllTab()) {
                // 전체 탭: 도메인별 그룹 뷰 (그룹당 groupSize건 + 총건수, 윈도우 쿼리 1회)
                List<SearchResultItem> rows = searchMapper.searchGrouped(
                        tsquery, fromTs, toTs, cond.getSort(), groupSize);
                res.setGroups(toGroups(rows));
                res.setTotal(grandTotal);
            } else {
                long total = res.getTabCounts().getOrDefault(docType, 0L);
                res.setItems(searchMapper.searchTab(tsquery, docType, blankToNull(cond.getCategory()),
                        fromTs, toTs, cond.getSort(), cond.getSize(), cond.offset()));
                res.setTotal(total);
                res.setHasMore((long) (cond.getPage() + 1) * cond.getSize() < total);
                res.setCategoryCounts(searchMapper.countByCategory(tsquery, docType, fromTs, toTs));
            }
            return null;
        });
        if (res.getTotal() == 0) {
            res.setMessage("검색 결과가 없습니다.");
            meterRegistry.counter("search.noresult").increment();
        }

        // 6) 하이라이트 (span: search.highlight) — 토큰 + 동의어 전부, escape 후 <mark>
        observe("search.highlight", () -> {
            Pattern pattern = highlightService.compile(highlightTerms);
            res.getItems().forEach(item -> applyHighlight(item, pattern));
            res.getGroups().forEach(g -> g.getItems().forEach(item -> applyHighlight(item, pattern)));
            return null;
        });

        long elapsed = System.currentTimeMillis() - start;
        res.setElapsedMs(elapsed);
        meterRegistry.summary("search.results").record(res.getTotal());
        stopQueryTimer(sample, docType, false);

        // 7) 로그 (@Async — 응답 지연 없음)
        logSearch(q, String.join(" ", highlightTerms), tsquery, cond,
                res.getTotal(), false, sessionId, clientIp, start);
        return res;
    }

    /** 토큰 리스트 → 검색식. 토큰별 동의어 그룹은 항상 OR, 그룹 간 결합은 op. 토큰 없으면 null. */
    private String buildExpr(List<String> tokens, String op,
                             BannedWordService.Snapshot banned,
                             Map<String, Set<String>> synonyms,
                             Set<String> highlightTerms) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        String joiner = "OR".equalsIgnoreCase(op) ? " | " : " & ";
        List<String> groups = new ArrayList<>();
        for (String token : tokens) {
            Set<String> expanded = new LinkedHashSet<>();
            expanded.add(token);
            expanded.addAll(synonyms.getOrDefault(token, Set.of()));
            expanded.removeIf(banned::isMasked);
            highlightTerms.addAll(expanded);
            groups.add(expanded.size() == 1
                    ? quote(token)
                    : "(" + expanded.stream().map(this::quote).collect(Collectors.joining(" | ")) + ")");
        }
        return String.join(joiner, groups);
    }

    /** tsquery 인젝션 방지: 토큰을 따옴표로 감싸고 내부 따옴표 제거 */
    private String quote(String token) {
        return "'" + token.replace("'", "").replace("\\", "") + "'";
    }

    private OffsetDateTime resolveFromTs(SearchCondition cond) {
        ZoneId zone = ZoneId.systemDefault();
        if (cond.getDateFrom() != null) {
            return cond.getDateFrom().atStartOfDay(zone).toOffsetDateTime();
        }
        LocalDate today = LocalDate.now(zone);
        return switch (cond.getPeriod() == null ? "all" : cond.getPeriod()) {
            case "6h" -> OffsetDateTime.now(zone).minusHours(6);
            case "1d" -> OffsetDateTime.now(zone).minusHours(24);
            case "week" -> today.with(DayOfWeek.MONDAY).atStartOfDay(zone).toOffsetDateTime();
            case "month" -> today.withDayOfMonth(1).atStartOfDay(zone).toOffsetDateTime();
            default -> null;
        };
    }

    private OffsetDateTime resolveToTs(SearchCondition cond) {
        if (cond.getDateTo() == null) {
            return null;
        }
        // 종료일 당일 전체 포함: dateTo + 1일 00:00 미만
        return cond.getDateTo().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /** 그룹 쿼리 결과 → 설정 순서(group-order)대로 그룹 구성 */
    private List<SearchGroup> toGroups(List<SearchResultItem> rows) {
        Map<String, List<SearchResultItem>> byType = rows.stream()
                .collect(Collectors.groupingBy(SearchResultItem::getDocType,
                        LinkedHashMap::new, Collectors.toList()));
        List<SearchGroup> groups = new ArrayList<>();
        for (String docType : Arrays.stream(groupOrderConfig.split(",")).map(String::trim).toList()) {
            List<SearchResultItem> items = byType.remove(docType);
            if (items != null && !items.isEmpty()) {
                groups.add(new SearchGroup(docType, items.get(0).getTypeTotal(), items));
            }
        }
        byType.forEach((type, items) ->                     // 설정에 없는 타입도 뒤에 노출
                groups.add(new SearchGroup(type, items.get(0).getTypeTotal(), items)));
        return groups;
    }

    private void applyHighlight(SearchResultItem item, Pattern pattern) {
        item.setTitle(highlightService.highlight(item.getTitle(), pattern));
        item.setSummary(highlightService.snippet(item.getSummary(), pattern));
    }

    private void logSearch(String q, String tokens, String tsquery, SearchCondition cond,
                           long resultCount, boolean blocked,
                           String sessionId, String clientIp, long start) {
        SearchKeywordLog entry = new SearchKeywordLog();
        entry.setKeyword(q);
        entry.setAnalyzedTokens(truncate(tokens, 500));
        entry.setExpandedQuery(truncate(tsquery, 1000));
        entry.setDocType(cond.isAllTab() ? null : cond.getType().toUpperCase());
        entry.setResultCount((int) Math.min(resultCount, Integer.MAX_VALUE));
        entry.setBlocked(blocked);
        entry.setSessionId(sessionId);
        entry.setTraceId(MDC.get("traceId"));
        entry.setElapsedMs((int) (System.currentTimeMillis() - start));
        entry.setCreatedIp(clientIp);               // @Async 스레드에서 서버 IP로 폴백되지 않도록 선세팅
        entry.setCreatedBy("guest");
        keywordLogService.logAsync(entry);
    }

    /** 단계별 span 기록 (search.analyze / expand / query / highlight) */
    private <T> T observe(String name, Supplier<T> supplier) {
        return Observation.createNotStarted(name, observationRegistry).observe(supplier);
    }

    /** 검색 전 과정 Timer — doc_type·blocked 태그로 p95/p99·처리량 분석 */
    private void stopQueryTimer(Timer.Sample sample, String docType, boolean blocked) {
        sample.stop(Timer.builder("search.query")
                .tag("doc_type", docType == null ? "ALL" : docType)
                .tag("blocked", String.valueOf(blocked))
                .description("검색 응답시간")
                .register(meterRegistry));
    }

    private String normalize(String q) {
        if (q == null) {
            return "";
        }
        String trimmed = q.strip();
        return trimmed.length() > MAX_QUERY_LENGTH ? trimmed.substring(0, MAX_QUERY_LENGTH) : trimmed;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String truncate(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
    }

    private List<String> concat(List<String> list, String last) {
        List<String> all = new ArrayList<>(list);
        all.add(last);
        return all;
    }
}
