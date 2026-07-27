package com.gonet.search.analyzer;

import com.gonet.search.domain.DicWord;
import com.gonet.search.mapper.DicWordMapper;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.ko.dict.UserDictionary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * tn_search_dic_word → Nori UserDictionary 변환 (DESIGN.md 4.2).
 * 포맷: 단일어 "아이폰15" / 복합명사 "검색엔진 검색 엔진" (word + 분해형 segments)
 */
@Component
@RequiredArgsConstructor
public class UserDictionaryLoader {

    private final DicWordMapper dicWordMapper;

    /** 활성 단어사전을 Nori UserDictionary로 로드한다. 등록 단어가 없으면 null. */
    public UserDictionary load() {
        List<DicWord> words = dicWordMapper.findAllEnabled();
        if (words.isEmpty()) {
            return null;
        }
        String lines = words.stream()
                .map(this::toLine)
                .collect(Collectors.joining("\n"));
        try {
            return UserDictionary.open(new StringReader(lines));
        } catch (IOException e) {
            throw new UncheckedIOException("Nori 사용자 사전 로딩 실패", e);
        }
    }

    private String toLine(DicWord word) {
        return (word.getSegments() == null || word.getSegments().isBlank())
                ? word.getWord()
                : word.getWord() + " " + word.getSegments();
    }
}
