package com.gonet.search.web.adm;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 모니터 대시보드 (관리자) — Spring/Micrometer(Prometheus) 메트릭을 Chart.js로 시각화.
 * 외부 연결 제한 환경 고려: Grafana 미사용, Chart.js는 webjar 내장, 데이터는 MeterRegistry 스냅샷 JSON 폴링.
 * ※ 권한(Spring Security)은 추후.
 */
@Controller
@RequestMapping("/adm/monitor")
@RequiredArgsConstructor
public class MonitorAdmController {

    private static final List<String> SPANS =
            List.of("search.analyze", "search.expand", "search.fts", "search.highlight");
    private static final List<String> DOC_TYPES = List.of("CONTENT", "FILE", "BBS", "MENU");

    private final MeterRegistry registry;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("menu", "monitor");
        return "adm/monitor";
    }

    /** 메트릭 스냅샷 JSON — 화면이 5초 폴링, 증가율(QPS 등)은 클라이언트가 차분 계산 */
    @GetMapping("/summary")
    @ResponseBody
    public Map<String, Object> summary() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("timestamp", System.currentTimeMillis());

        // 검색 전체 (search.query Timer — doc_type/blocked 태그 합산)
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("count", sumTimerCount("search.query"));
        search.put("totalTimeMs", sumTimerTotalMs("search.query"));
        search.put("maxMs", registry.find("search.query").timers().stream()
                .mapToDouble(t -> t.max(TimeUnit.MILLISECONDS)).max().orElse(0));
        search.put("noresult", counterValue("search.noresult"));
        search.put("blocked", counterValue("search.blocked"));
        out.put("search", search);

        // 단계별 span (ms)
        Map<String, Object> spans = new LinkedHashMap<>();
        for (String name : SPANS) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("count", sumTimerCount(name));
            s.put("totalTimeMs", sumTimerTotalMs(name));
            spans.put(name.substring("search.".length()), s);
        }
        out.put("spans", spans);

        // 캐시 히트/미스 (Caffeine → cache.gets FunctionCounter)
        Map<String, Map<String, Double>> caches = new LinkedHashMap<>();
        for (FunctionCounter fc : registry.find("cache.gets").functionCounters()) {
            String cache = fc.getId().getTag("cache");
            String result = fc.getId().getTag("result");
            if (cache == null || result == null) {
                continue;
            }
            caches.computeIfAbsent(cache, k -> new LinkedHashMap<>())
                    .merge(result, fc.count(), Double::sum);
        }
        out.put("caches", caches);

        // 색인 문서 수 (도메인별 Gauge)
        Map<String, Object> index = new LinkedHashMap<>();
        for (String docType : DOC_TYPES) {
            Gauge gauge = registry.find("index.documents").tag("doc_type", docType).gauge();
            index.put(docType, gauge == null ? 0 : gauge.value());
        }
        out.put("indexDocuments", index);

        // 배치 소요 (색인 동기화 mode별 · 인기검색어 MV 갱신)
        Map<String, Object> batch = new LinkedHashMap<>();
        for (String mode : List.of("diff", "full")) {
            Timer timer = registry.find("index.sync").tag("mode", mode).timer();
            if (timer != null && timer.count() > 0) {
                batch.put("sync." + mode + ".meanMs", timer.mean(TimeUnit.MILLISECONDS));
                batch.put("sync." + mode + ".count", timer.count());
            }
        }
        Timer refresh = registry.find("keyword.popular.refresh").timer();
        if (refresh != null && refresh.count() > 0) {
            batch.put("popularRefresh.meanMs", refresh.mean(TimeUnit.MILLISECONDS));
            batch.put("popularRefresh.count", refresh.count());
        }
        out.put("batch", batch);

        return out;
    }

    private double sumTimerCount(String name) {
        return registry.find(name).timers().stream().mapToDouble(Timer::count).sum();
    }

    private double sumTimerTotalMs(String name) {
        return registry.find(name).timers().stream()
                .mapToDouble(t -> t.totalTime(TimeUnit.MILLISECONDS)).sum();
    }

    private double counterValue(String name) {
        Counter counter = registry.find(name).counter();
        return counter == null ? 0 : counter.count();
    }
}
