package com.gonet.search.service;

import com.gonet.search.extract.FileTextExtractor;
import com.gonet.search.mapper.FileMapper;
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
 *      → **개인정보 마스킹** → tn_file.extract_text UPDATE(기존 값과 같으면 건너뜀)
 *      → content_hash가 바뀌므로 색인 동기화(diff)를 이어서 호출 — 즉시 검색 반영
 *
 * - 스케줄: 매일 새벽 1시(search.extract.cron, 최근 schedule-days일)
 *   + 어드민 [파일 추출] 버튼(최근 manual-months개월)
 * - DB 반영 전 반드시 MaskingUtil을 거친다 (색인 DB에 개인정보 미저장 정책과 동일)
 * - 추출 결과를 원본(tn_file.extract_text)에 저장하므로 전체 재색인·diff 동기화 후에도 유지된다
 * - 색인 동기화·재색인과 IndexJobLock을 공유 — 동시 실행 레이스·중복 실행 방지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileExtractService {

    private final FileMapper fileMapper;
    private final FileTextExtractor fileTextExtractor;
    private final IndexingService indexingService;
    private final IndexJobLock jobLock;

    @Value("${search.extract.schedule-days:3}")
    private int scheduleDays;

    @Value("${search.extract.manual-months:1}")
    private int manualMonths;

    /** 마지막 실행 결과 (어드민 화면 표시용) */
    @lombok.Getter
    private volatile ExtractResult lastResult;

    /** 매일 새벽 1시 자동 추출 — 매일 실행되므로 최근 N일(기본 3일)만 비교·추출 */
    @Scheduled(cron = "${search.extract.cron}")
    public void scheduledExtract() {
        extract(OffsetDateTime.now().minusDays(scheduleDays), "스케줄 · 최근 " + scheduleDays + "일");
    }

    /** 어드민 [파일 추출] 버튼 — 최근 N개월(기본 1개월) 대상. 다른 색인 작업 실행 중이면 null. */
    public ExtractResult extractRecent() {
        return extract(OffsetDateTime.now().minusMonths(manualMonths), "수동 · 최근 " + manualMonths + "개월");
    }

    /** 파일 텍스트 추출 → tn_file.extract_text 반영 → 색인 동기화 */
    private ExtractResult extract(OffsetDateTime fromTs, String mode) {
        if (!jobLock.tryLock()) {
            log.warn("다른 색인 작업이 실행 중 — 파일 추출 건너뜀({})", mode);
            return null;
        }
        try {
            long start = System.currentTimeMillis();
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

                    // 기존 값과 같으면 미갱신 — updated_at이 안 바뀌어야 추출 윈도우에 매일 다시 잡히지 않는다
                    if (masked.equals(target.getExtractText())) {
                        skipped++;
                        continue;
                    }
                    target.setExtractText(masked);
                    fileMapper.updateExtractText(target);
                    extracted++;
                } catch (Exception e) {
                    failed++;
                    log.warn("파일 텍스트 추출 실패: id={}, path={}", target.getId(), target.getFilePath(), e);
                }
            }
            // extract_text 변경으로 content_hash가 바뀐 문서를 즉시 색인 (ReentrantLock — 같은 스레드라 통과)
            if (extracted > 0) {
                indexingService.syncSearchIndex();
            }
            long elapsed = System.currentTimeMillis() - start;
            log.info("파일 텍스트 추출 완료({}): 대상 {}건 중 반영 {}건, 건너뜀 {}건, 실패 {}건, {}ms",
                    mode, targets.size(), extracted, skipped, failed, elapsed);
            lastResult = new ExtractResult(mode, targets.size(), extracted, skipped, failed, elapsed);
            return lastResult;
        } finally {
            jobLock.unlock();
        }
    }

    /** 추출 결과 요약 (어드민 표시용) */
    public record ExtractResult(String mode, int targets, int extracted, int skipped, int failed, long elapsedMs) {
    }
}
