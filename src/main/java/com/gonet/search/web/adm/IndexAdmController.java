package com.gonet.search.web.adm;

import com.gonet.search.config.SearchDocTypes;
import com.gonet.search.mapper.SearchIndexMapper;
import com.gonet.search.service.FileExtractService;
import com.gonet.search.service.IndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 색인 관리 — 도메인별 색인 건수 + 지금 동기화(해시 diff) / 전체 재색인(전량 재분석).
 * ※ 권한(Spring Security)은 추후.
 */
@Controller
@RequestMapping("/adm/index")
@RequiredArgsConstructor
public class IndexAdmController {

    private final IndexingService indexingService;
    private final FileExtractService fileExtractService;
    private final SearchIndexMapper searchIndexMapper;
    private final SearchDocTypes docTypes;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("menu", "index");
        model.addAttribute("docTypes", docTypes);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String docType : docTypes.codes()) {
            counts.put(docType, searchIndexMapper.count(docType));
        }
        model.addAttribute("counts", counts);
        model.addAttribute("total", searchIndexMapper.count(null));
        model.addAttribute("lastResult", indexingService.getLastResult());
        model.addAttribute("lastExtract", fileExtractService.getLastResult());
        return "adm/index";
    }

    /** 지금 동기화 — content_hash diff (변경분만 재분석) */
    @PostMapping("/sync")
    public String sync(RedirectAttributes ra) {
        warnIfBusy(ra, indexingService.syncSearchIndex() == null);
        return "redirect:/adm/index";
    }

    /** 전체 재색인 — 해시 무시 전량 재분석 (사전·품사 설정 변경 후) */
    @PostMapping("/rebuild")
    public String rebuild(RedirectAttributes ra) {
        warnIfBusy(ra, indexingService.rebuildAll() == null);
        return "redirect:/adm/index";
    }

    /** 파일 추출 — 최근 1개월 파일의 본문을 tn_file.extract_text에 반영 후 색인 동기화 (마스킹 포함) */
    @PostMapping("/extract")
    public String extract(RedirectAttributes ra) {
        warnIfBusy(ra, fileExtractService.extractRecent() == null);
        return "redirect:/adm/index";
    }

    /** IndexJobLock 획득 실패(null 반환) 시 안내 배너 */
    private void warnIfBusy(RedirectAttributes ra, boolean busy) {
        if (busy) {
            ra.addFlashAttribute("message", "다른 색인 작업이 실행 중입니다. 완료 후 다시 시도해 주세요.");
        }
    }
}
