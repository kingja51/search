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
 * 흐름: 최근 파일(선언 확장자만) 조회 → 원본파일전체경로에서 본문 추출(Tika/hwplib)
 *      → **개인정보 마스킹** → 반영 (search.extract.update-origin 설정으로 선택)
 *      - false(기본): tn_search_index(summary·tokens) 직접 UPDATE — 원본 불변, 해시 유지.
 *        ⚠️ 전체 재색인 시 추출 본문이 초기화되므로 재색인 후 재실행 필요
 *      - true: tn_file.extract_text UPDATE(기존 값과 같으면 건너뜀) → content_hash가 바뀌므로
 *        색인 동기화(diff)를 이어서 호출 — 재색인 후에도 유지, 파일 뷰어에도 노출
 *
 * - 스케줄: 매일 새벽 1시(search.extract.cron, 최근 schedule-days일)
 *   + 어드민 [파일 추출] 버튼(최근 manual-months개월)
 * - DB 반영 전 반드시 MaskingUtil을 거친다 (색인 DB에 개인정보 미저장 정책과 동일)
 * - 색인 동기화·재색인과 IndexJobLock을 공유 — 동시 실행 레이스·중복 실행 방지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileExtractService {

    private final FileMapper fileMapper;
    private final SearchIndexMapper searchIndexMapper;
    private final FileTextExtractor fileTextExtractor;
    private final KoreanAnalyzer koreanAnalyzer;
    private final IndexingService indexingService;
    private final IndexJobLock jobLock;

    @Value("${search.extract.schedule-days:3}")
    private int scheduleDays;

    @Value("${search.extract.manual-months:1}")
    private int manualMonths;

    @Value("${search.extract.update-origin:false}")
    private boolean updateOrigin;

    @Value("${search.result.summary-length:2000}")
    private int summaryLength;

    /** 마지막 실행 결과 (어드민 화면 표시용) */
    @lombok.Getter
    private volatile ExtractResult lastResult;

    /** 현재 반영 방식 (어드민 화면 안내용) — true: 원본 저장 / false: 색인만 갱신 */
    public boolean isUpdateOrigin() {
        return updateOrigin;
    }

    /** 매일 새벽 1시 자동 추출 — 매일 실행되므로 최근 N일(기본 3일)만 비교·추출 */
    @Scheduled(cron = "${search.extract.cron}")
    public void scheduledExtract() {
        extract(OffsetDateTime.now().minusDays(scheduleDays), "스케줄 · 최근 " + scheduleDays + "일");
    }

    /** 어드민 [파일 추출] 버튼 — 최근 N개월(기본 1개월) 대상. 다른 색인 작업 실행 중이면 null. */
    public ExtractResult extractRecent() {
        return extract(OffsetDateTime.now().minusMonths(manualMonths), "수동 · 최근 " + manualMonths + "개월");
    }

    /** 파일 텍스트 추출 → 반영(update-origin 설정에 따라 원본 또는 색인) */
    private ExtractResult extract(OffsetDateTime fromTs, String mode) {
        if (!jobLock.tryLock()) {
            log.warn("다른 색인 작업이 실행 중 — 파일 추출 건너뜀({})", mode);
            return null;
        }
        try {
            long start = System.currentTimeMillis();
            String ip = ClientIpHolder.get();
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
                    String masked = MaskingUtil.mask(text == null ? "" : text);

                    if (updateOrigin ? applyToOrigin(target, masked) : applyToIndex(target, masked, ip)) {
                        extracted++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    failed++;
                    log.warn("파일 텍스트 추출 실패: id={}, path={}", target.getId(), target.getFilePath(), e);
                }
            }
            // 원본 저장 방식: extract_text 변경으로 content_hash가 바뀐 문서를 즉시 색인 (ReentrantLock — 같은 스레드라 통과)
            if (updateOrigin && extracted > 0) {
                indexingService.syncSearchIndex();
            }
            long elapsed = System.currentTimeMillis() - start;
            log.info("파일 텍스트 추출 완료({}, update-origin={}): 대상 {}건 중 반영 {}건, 건너뜀 {}건, 실패 {}건, {}ms",
                    mode, updateOrigin, targets.size(), extracted, skipped, failed, elapsed);
            lastResult = new ExtractResult(mode, targets.size(), extracted, skipped, failed, elapsed);
            return lastResult;
        } finally {
            jobLock.unlock();
        }
    }

    /** update-origin=true: tn_file.extract_text 저장. 기존 값과 같으면 미갱신(updated_at 불변 — 재추출 루프 방지) */
    private boolean applyToOrigin(com.gonet.search.domain.File target, String masked) {
        if (masked.equals(target.getExtractText())) {
            return false;
        }
        target.setExtractText(masked);
        fileMapper.updateExtractText(target);
        return true;
    }

    /** update-origin=false(기본): tn_search_index(summary·tokens) 직접 갱신 — 원본 불변, content_hash 유지 */
    private boolean applyToIndex(com.gonet.search.domain.File target, String masked, String ip) {
        String maskedTitle = MaskingUtil.mask(target.getFileName());
        String summary = truncateSafely(masked, summaryLength);
        String tokens = String.join(" ", koreanAnalyzer.analyze(maskedTitle + " " + masked));
        int updated = searchIndexMapper.updateFileContent(target.getId(), summary, tokens, ip);
        if (updated == 0) {
            log.warn("색인에 없는 파일 — 색인 동기화 후 재시도 필요: id={}", target.getId());
            return false;
        }
        return true;
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
    public record ExtractResult(String mode, int targets, int extracted, int skipped, int failed, long elapsedMs) {
    }
}
