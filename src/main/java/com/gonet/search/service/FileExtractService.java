package com.gonet.search.service;

import com.gonet.search.analyzer.KoreanAnalyzer;
import com.gonet.search.config.ClientIpHolder;
import com.gonet.search.extract.FileTextExtractor;
import com.gonet.search.mapper.FileMapper;
import com.gonet.search.mapper.SearchIndexMapper;
import com.gonet.search.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 파일 텍스트 추출 배치 (DESIGN.md 4.4 파일 텍스트 추출).
 *
 * 흐름: 최근 N개월 파일(선언 확장자만) 조회 → 원본파일전체경로에서 본문 추출(Tika/hwplib)
 *      → **개인정보 마스킹** → Nori 분석 → tn_search_index(summary·tokens) 갱신
 *
 * - 스케줄: 매일 새벽 1시 (search.extract.cron) + 어드민 [파일 추출] 버튼 수동 실행
 * - DB 반영 전 반드시 MaskingUtil을 거친다 (색인 DB에 개인정보 미저장 정책과 동일)
 * - ⚠️ 전체 재색인은 vw_search_source(extract_text) 기준으로 색인을 다시 만들므로
 *   추출 반영 내용이 초기화된다 — 재색인 후에는 파일 추출을 다시 실행할 것
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileExtractService {

    private final FileMapper fileMapper;
    private final SearchIndexMapper searchIndexMapper;
    private final FileTextExtractor fileTextExtractor;
    private final KoreanAnalyzer koreanAnalyzer;

    @Value("${search.extract.months:1}")
    private int months;

    @Value("${search.result.summary-length:2000}")
    private int summaryLength;

    /** 마지막 실행 결과 (어드민 화면 표시용) */
    @lombok.Getter
    private volatile ExtractResult lastResult;

    /** 매일 새벽 1시 자동 추출 */
    @Scheduled(cron = "${search.extract.cron}")
    public void scheduledExtract() {
        extractRecent();
    }

    /** 최근 N개월 파일 텍스트 추출 → 색인 반영 */
    public synchronized ExtractResult extractRecent() {
        long start = System.currentTimeMillis();
        String ip = ClientIpHolder.get();
        OffsetDateTime fromTs = OffsetDateTime.now().minusMonths(months);
        List<com.gonet.search.domain.File> targets =
                fileMapper.findExtractTargets(fromTs, fileTextExtractor.allowedExtensions());

        int extracted = 0;
        int skipped = 0;
        int failed = 0;
        for (com.gonet.search.domain.File target : targets) {
            try {
                File file = new File(target.getFilePath());
                if (!file.isFile() || !file.canRead()) {
                    skipped++;
                    log.warn("파일 없음/읽기 불가 — 건너뜀: id={}, path={}", target.getId(), target.getFilePath());
                    continue;
                }
                String text = fileTextExtractor.extract(file, target.getFileExt());

                // DB 반영 전 개인정보 마스킹 (필수)
                String maskedText = MaskingUtil.mask(text == null ? "" : text);
                String maskedTitle = MaskingUtil.mask(target.getFileName());

                String summary = truncateSafely(maskedText, summaryLength);
                String tokens = String.join(" ", koreanAnalyzer.analyze(maskedTitle + " " + maskedText));

                int updated = searchIndexMapper.updateFileContent(target.getId(), summary, tokens, ip);
                if (updated == 0) {
                    skipped++;
                    log.warn("색인에 없는 파일 — 색인 동기화 후 재시도 필요: id={}", target.getId());
                } else {
                    extracted++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("파일 텍스트 추출 실패: id={}, path={}", target.getId(), target.getFilePath(), e);
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        log.info("파일 텍스트 추출 완료: 대상 {}건 중 반영 {}건, 건너뜀 {}건, 실패 {}건, {}ms",
                targets.size(), extracted, skipped, failed, elapsed);
        lastResult = new ExtractResult(targets.size(), extracted, skipped, failed, elapsed);
        return lastResult;
    }

    /** 절단 경계가 서로게이트 페어를 가르지 않게 요약을 자른다 */
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

    /** 추출 결과 요약 (어드민 표시용) */
    public record ExtractResult(int targets, int extracted, int skipped, int failed, long elapsedMs) {
    }
}
