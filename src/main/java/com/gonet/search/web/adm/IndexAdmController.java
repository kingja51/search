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
    public String sync() {
        indexingService.syncSearchIndex();
        return "redirect:/adm/index";
    }

    /** 전체 재색인 — 해시 무시 전량 재분석 (사전·품사 설정 변경 후) */
    @PostMapping("/rebuild")
    public String rebuild() {
        indexingService.rebuildAll();
        return "redirect:/adm/index";
    }

    /** 파일 추출 — 최근 1개월 파일의 본문을 원본 경로에서 추출해 색인에 반영 (마스킹 포함) */
    @PostMapping("/extract")
    public String extract() {
        fileExtractService.extractRecent();
        return "redirect:/adm/index";
    }
}
