package com.gonet.search.service;

import com.gonet.search.domain.DicBanned;
import com.gonet.search.mapper.DicBannedMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 금지어 필터 (DESIGN.md 4.3 - 2단계).
 * BLOCK: 검색 자체 차단 / MASK: 검색은 허용하되 해당 토큰을 검색식에서 제외(결과 노출 제한).
 * 스냅샷은 bannedWords 캐시(수동 무효화 — DictionaryService.reloadDictionaries)로 서빙.
 */
@Service
@RequiredArgsConstructor
public class BannedWordService {

    private final DicBannedMapper dicBannedMapper;

    /** 금지어 스냅샷 — 캐시 1엔트리, 사전 변경 시 reloadDictionaries()가 evict */
    @Cacheable(cacheNames = "bannedWords", key = "'all'")
    public Snapshot snapshot() {
        Set<String> blocked = new HashSet<>();
        Set<String> masked = new HashSet<>();
        for (DicBanned banned : dicBannedMapper.findAllEnabled()) {
            String word = banned.getWord().toLowerCase(Locale.ROOT);
            if ("MASK".equalsIgnoreCase(banned.getBlockType())) {
                masked.add(word);
            } else {
                blocked.add(word);
            }
        }
        return new Snapshot(blocked, masked);
    }

    public record Snapshot(Set<String> blocked, Set<String> masked) {

        /** BLOCK 금지어가 검색어에 포함되어 있으면 해당 단어 반환 (부분 문자열 매칭) */
        public Optional<String> findBlocked(String query) {
            if (query == null || query.isBlank()) {
                return Optional.empty();
            }
            String lower = query.toLowerCase(Locale.ROOT);
            return blocked.stream().filter(lower::contains).findFirst();
        }

        /** MASK 대상 토큰인지 (검색식에서 제외) */
        public boolean isMasked(String token) {
            return masked.contains(token.toLowerCase(Locale.ROOT));
        }
    }
}
