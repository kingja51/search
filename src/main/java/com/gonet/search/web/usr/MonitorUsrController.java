package com.gonet.search.web.usr;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 내장 모니터 대시보드 (Chart.js — 외부 연결 제한 환경 고려, Grafana 미사용).
 * 데이터는 /api/monitor/summary 를 5초 폴링. 권한 제한은 Spring Security 도입 시(추후) 적용.
 */
@Controller
public class MonitorUsrController {

    @GetMapping("/monitor")
    public String monitor() {
        return "usr/monitor";
    }
}
