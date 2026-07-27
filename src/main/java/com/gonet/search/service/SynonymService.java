package com.gonet.search.service;

import com.gonet.search.domain.DicSynonym;
import com.gonet.search.mapper.DicSynonymMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 동의어 확장 (DESIGN.md 4.3 - 4단계). 같은 group_id = 서로 동의어, 확장은 항상 그룹 내 OR.
 * 확장 맵 전체를 synonyms 캐시 1엔트리로 서빙 (수동 무효화 — DictionaryService.reloadDictionaries).
 */
@Service
@RequiredArgsConstructor
public class SynonymService {

    private final DicSynonymMapper dicSynonymMapper;

    /** 단어 → 동의어 그룹 전체(자기 자신 포함) 맵 — 캐시 1엔트리 */
    @Cacheable(cacheNames = "synonyms", key = "'all'")
    public Map<String, Set<String>> expansionMap() {
        List<DicSynonym> all = dicSynonymMapper.findAllEnabled();

        Map<Long, Set<String>> groups = new HashMap<>();
        for (DicSynonym synonym : all) {
            groups.computeIfAbsent(synonym.getGroupId(), k -> new LinkedHashSet<>())
                    .add(synonym.getWord());
        }

        Map<String, Set<String>> byWord = new HashMap<>();
        for (Set<String> group : groups.values()) {
            for (String word : group) {
                byWord.merge(word, group, (a, b) -> {
                    Set<String> merged = new LinkedHashSet<>(a);
                    merged.addAll(b);
                    return merged;
                });
            }
        }
        return byWord;
    }
}
