package com.gonet.search.web.adm;

import com.gonet.search.domain.DicBanned;
import com.gonet.search.domain.DicSynonym;
import com.gonet.search.domain.DicWord;
import com.gonet.search.domain.RecommendKeyword;
import com.gonet.search.service.DictionaryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.time.LocalDate;
import java.util.Set;

/**
 * 사전 관리 (단어/동의어/금지어/추천 검색어) — 목록 + 등록/활성토글/삭제.
 * 변경은 트랜잭션 커밋 후 캐시 evict + Nori 리로드가 자동 수행된다 (DictionaryService).
 * ※ 권한(Spring Security)은 추후 — 도입 시 /adm/** 접근 제한.
 */
@Controller
@RequestMapping("/adm/dic")
@RequiredArgsConstructor
public class DicAdmController {

    private static final Set<String> TYPES = Set.of("word", "synonym", "banned", "recommend");

    private final DictionaryService dictionaryService;

    /**
     * UNIQUE 제약 위반(중복 등록) → 500 대신 목록으로 돌아가 안내 메시지 표시.
     * @ExceptionHandler는 RedirectAttributes 파라미터를 지원하지 않으므로 OutputFlashMap에 직접 기록.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public String duplicate(HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request)
                .put("error", "이미 등록된 항목입니다. (중복 등록 불가)");
        String uri = request.getRequestURI();
        String type = uri.substring(uri.lastIndexOf('/') + 1);
        return "redirect:/adm/dic/" + (TYPES.contains(type) ? type : "word");
    }

    @GetMapping("/{type}")
    public String list(@PathVariable String type, Model model) {
        model.addAttribute("menu", type);
        model.addAttribute("type", type);
        switch (type) {
            case "word" -> model.addAttribute("items", dictionaryService.words());
            case "synonym" -> model.addAttribute("items", dictionaryService.synonyms());
            case "banned" -> model.addAttribute("items", dictionaryService.banned());
            case "recommend" -> model.addAttribute("items", dictionaryService.recommends());
            default -> {
                return "redirect:/adm/dic/word";
            }
        }
        return "adm/dic-list";
    }

    /* ── 등록 (타입별 필드) ── */

    @PostMapping("/word")
    public String addWord(@RequestParam String word,
                          @RequestParam(required = false) String segments,
                          @RequestParam(defaultValue = "NNG") String posTag,
                          @RequestParam(required = false) String memo) {
        DicWord entity = new DicWord();
        entity.setWord(word.strip());
        entity.setSegments(blankToNull(segments));
        entity.setPosTag(posTag);
        entity.setMemo(blankToNull(memo));
        dictionaryService.addWord(entity);
        return "redirect:/adm/dic/word";
    }

    @PostMapping("/synonym")
    public String addSynonym(@RequestParam Long groupId,
                             @RequestParam String word,
                             @RequestParam(defaultValue = "false") boolean representative) {
        DicSynonym entity = new DicSynonym();
        entity.setGroupId(groupId);
        entity.setWord(word.strip());
        entity.setRepresentative(representative);
        dictionaryService.addSynonym(entity);
        return "redirect:/adm/dic/synonym";
    }

    @PostMapping("/banned")
    public String addBanned(@RequestParam String word,
                            @RequestParam(defaultValue = "BLOCK") String blockType,
                            @RequestParam(required = false) String memo) {
        DicBanned entity = new DicBanned();
        entity.setWord(word.strip());
        entity.setBlockType(blockType);
        entity.setMemo(blankToNull(memo));
        dictionaryService.addBanned(entity);
        return "redirect:/adm/dic/banned";
    }

    @PostMapping("/recommend")
    public String addRecommend(@RequestParam String keyword,
                               @RequestParam(defaultValue = "0") int displayOrder,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate,
                               @RequestParam(required = false) String memo) {
        RecommendKeyword entity = new RecommendKeyword();
        entity.setKeyword(keyword.strip());
        entity.setDisplayOrder(displayOrder);
        entity.setStartDate(parseDate(startDate));
        entity.setEndDate(parseDate(endDate));
        entity.setMemo(blankToNull(memo));
        dictionaryService.addRecommend(entity);
        return "redirect:/adm/dic/recommend";
    }

    /* ── 활성 토글 / 삭제 ── */

    @PostMapping("/{type}/{id}/toggle")
    public String toggle(@PathVariable String type, @PathVariable Long id) {
        switch (type) {
            case "word" -> dictionaryService.toggleWord(id);
            case "synonym" -> dictionaryService.toggleSynonym(id);
            case "banned" -> dictionaryService.toggleBanned(id);
            case "recommend" -> dictionaryService.toggleRecommend(id);
            default -> { }
        }
        return "redirect:/adm/dic/" + type;
    }

    @PostMapping("/{type}/{id}/delete")
    public String delete(@PathVariable String type, @PathVariable Long id) {
        switch (type) {
            case "word" -> dictionaryService.deleteWord(id);
            case "synonym" -> dictionaryService.deleteSynonym(id);
            case "banned" -> dictionaryService.deleteBanned(id);
            case "recommend" -> dictionaryService.deleteRecommend(id);
            default -> { }
        }
        return "redirect:/adm/dic/" + type;
    }

    /** 사전 리로드 수동 트리거 (SQL로 직접 수정한 경우 등) */
    @PostMapping("/reload")
    public String reload() {
        dictionaryService.reloadDictionaries();
        return "redirect:/adm/dic/word";
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }

    private LocalDate parseDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
    }
}
