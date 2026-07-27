package com.gonet.search.web.usr;

import com.gonet.search.domain.Bbs;
import com.gonet.search.domain.Content;
import com.gonet.search.domain.File;
import com.gonet.search.mapper.BbsMapper;
import com.gonet.search.mapper.ContentMapper;
import com.gonet.search.mapper.FileMapper;
import com.gonet.search.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 샘플 원본 상세 뷰어 — 검색 결과 link_url(/content/{id}, /bbs/{board}/{id}, /file/{id})의 목적지.
 * 검색엔진 데모용 최소 화면 (실제 서비스에서는 각 도메인의 화면으로 대체된다).
 * 원본을 직접 출력하므로 표시 직전에 개인정보 마스킹을 적용한다 (색인 경로는 색인 시점에 마스킹됨).
 */
@Controller
@RequiredArgsConstructor
public class SampleViewUsrController {

    private final ContentMapper contentMapper;
    private final BbsMapper bbsMapper;
    private final FileMapper fileMapper;

    @GetMapping("/content/{id}")
    public String content(@PathVariable Long id, Model model) {
        Content content = contentMapper.findById(id);
        if (content != null) {
            content.setTitle(MaskingUtil.mask(content.getTitle()));
            content.setContent(MaskingUtil.mask(content.getContent()));
        }
        model.addAttribute("content", content);
        return "usr/view-content";
    }

    @GetMapping("/bbs/{boardCd}")
    public String bbsList(@PathVariable String boardCd, Model model) {
        List<Bbs> posts = bbsMapper.findByBoardCd(boardCd);
        posts.forEach(post -> post.setTitle(MaskingUtil.mask(post.getTitle())));
        model.addAttribute("boardCd", boardCd);
        model.addAttribute("posts", posts);
        return "usr/view-bbs-list";
    }

    @GetMapping("/bbs/{boardCd}/{id}")
    public String bbsDetail(@PathVariable String boardCd, @PathVariable Long id, Model model) {
        Bbs post = bbsMapper.findById(id);
        if (post != null) {
            post.setTitle(MaskingUtil.mask(post.getTitle()));
            post.setContent(MaskingUtil.mask(post.getContent()));
        }
        model.addAttribute("boardCd", boardCd);
        model.addAttribute("post", post);
        return "usr/view-bbs";
    }

    @GetMapping("/file/{id}")
    public String file(@PathVariable Long id, Model model) {
        File file = fileMapper.findById(id);
        if (file != null) {
            file.setFileName(MaskingUtil.mask(file.getFileName()));
            file.setExtractText(MaskingUtil.mask(file.getExtractText()));
        }
        model.addAttribute("file", file);
        return "usr/view-file";
    }
}
