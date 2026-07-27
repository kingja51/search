package com.gonet.search.web.usr;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 사용자 검색 화면. 검색 결과(/result)는 3단계(검색 코어)에서 구현한다. */
@Controller
public class SearchUsrController {

    @GetMapping("/")
    public String main() {
        return "usr/main";
    }
}
