package com.gonet.search.web.usr;

import com.gonet.search.mapper.BbsMapper;
import com.gonet.search.mapper.ContentMapper;
import com.gonet.search.mapper.FileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 샘플 원본 상세 뷰어 — 검색 결과 link_url(/content/{id}, /bbs/{board}/{id}, /file/{id})의 목적지.
 * 검색엔진 데모용 최소 화면 (실제 서비스에서는 각 도메인의 화면으로 대체된다).
 */
@Controller
@RequiredArgsConstructor
public class SampleViewUsrController {

    private final ContentMapper contentMapper;
    private final BbsMapper bbsMapper;
    private final FileMapper fileMapper;

    @GetMapping("/content/{id}")
    public String content(@PathVariable Long id, Model model) {
        model.addAttribute("content", contentMapper.findById(id));
        return "usr/view-content";
    }

    @GetMapping("/bbs/{boardCd}")
    public String bbsList(@PathVariable String boardCd, Model model) {
        model.addAttribute("boardCd", boardCd);
        model.addAttribute("posts", bbsMapper.findByBoardCd(boardCd));
        return "usr/view-bbs-list";
    }

    @GetMapping("/bbs/{boardCd}/{id}")
    public String bbsDetail(@PathVariable String boardCd, @PathVariable Long id, Model model) {
        model.addAttribute("boardCd", boardCd);
        model.addAttribute("post", bbsMapper.findById(id));
        return "usr/view-bbs";
    }

    @GetMapping("/file/{id}")
    public String file(@PathVariable Long id, Model model) {
        model.addAttribute("file", fileMapper.findById(id));
        return "usr/view-file";
    }
}
