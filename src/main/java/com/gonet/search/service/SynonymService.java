package com.gonet.search.service;

import com.gonet.search.domain.DicSynonym;
import com.gonet.search.mapper.DicSynonymMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 동의어 확장 (DESIGN.md 4.3 - 4단계). 같은 group_id = 서로 동의어, 확장은 항상 그룹 내 OR.
 * ※ 5단계에서 synonyms 캐시 적용 예정 — 지금은 요청당 1회 스냅샷 조회.
 */
@Service
@RequiredArgsConstructor
public class SynonymService {

    private final DicSynonymMapper dicSynonymMapper;

    /** 단어 → 동의어 그룹 전체(자기 자신 포함) 맵 스냅샷 */
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
