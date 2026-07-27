package com.gonet.search.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Caffeine 캐시 구성 (DESIGN.md 5장).
 * recordStats() 필수 — cache.gets{result=hit|miss} 등 Micrometer 메트릭 자동 노출.
 *
 * | 캐시               | 내용                          | TTL   | 무효화                       |
 * | synonyms          | 동의어 확장 맵 전체 (1엔트리)    | 수동   | 사전 변경 시 evict (reload)  |
 * | bannedWords       | 금지어 스냅샷 (1엔트리)         | 수동   | 사전 변경 시 evict (reload)  |
 * | popularKeywords   | 인기 검색어 TOP N              | 1분   | TTL 자동                    |
 * | recommendKeywords | 추천 검색어 노출분              | 10분  | TTL 자동 + 변경 시 evict     |
 * | autocomplete      | 접두어 → 자동완성 후보           | 5분   | TTL 자동                    |
 * ※ searchFirstPage(검색 1페이지 캐시)는 v1.0 보류 — 캐시 히트 시 검색 로그가 누락되어
 *   인기 검색어 집계가 왜곡되므로, 필요해지면 로그와 분리된 FTS 레이어 캐시로 재설계한다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache("synonyms",              1, null),
                buildCache("bannedWords",           1, null),
                buildCache("popularKeywords",      10, Duration.ofMinutes(1)),
                buildCache("recommendKeywords",     1, Duration.ofMinutes(10)),
                buildCache("autocomplete",      5_000, Duration.ofMinutes(5))
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, long maxSize, Duration ttl) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .recordStats();                  // ★ Micrometer 캐시 메트릭 필수
        if (ttl != null) {
            builder.expireAfterWrite(ttl);
        }
        return new CaffeineCache(name, builder.build());
    }
}
