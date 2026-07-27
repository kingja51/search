package com.gonet.search.config;

import com.gonet.search.analyzer.KoreanAnalyzer;
import com.gonet.search.analyzer.UserDictionaryLoader;
import org.apache.lucene.analysis.ko.POS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Nori 분석기 빈 구성 (DESIGN.md 4.2).
 * 품사 keep-list는 search.analyzer.keep-pos 설정으로 외부화 (변경 시 전체 재색인 필요).
 * 색인·검색이 이 단일 빈을 공유한다.
 */
@Configuration
public class NoriConfig {

    @Bean
    public KoreanAnalyzer koreanAnalyzer(UserDictionaryLoader userDictionaryLoader,
                                         @Value("${search.analyzer.keep-pos}") String keepPos) {
        Set<POS.Tag> keepTags = Arrays.stream(keepPos.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(POS.Tag::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(POS.Tag.class)));
        return new KoreanAnalyzer(keepTags, userDictionaryLoader.load());
    }
}
