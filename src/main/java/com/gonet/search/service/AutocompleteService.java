package com.gonet.search.service;

import com.gonet.search.mapper.SearchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/** 자동완성 — pg_trgm 유사도 추천, autocomplete 캐시(TTL 5분, 접두어별 키) */
@Service
@RequiredArgsConstructor
public class AutocompleteService {

    private final SearchMapper searchMapper;

    @Cacheable(cacheNames = "autocomplete", key = "#q")
    public List<String> suggest(String q, int limit) {
        return searchMapper.autocomplete(q, limit);
    }
}
