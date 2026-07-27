package com.gonet.search.analyzer;

import org.apache.lucene.analysis.ko.POS;
import org.apache.lucene.analysis.ko.dict.UserDictionary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Nori 래퍼 검증 — DB 없이 실행되는 순수 단위 테스트 */
class KoreanAnalyzerTest {

    private static final Set<POS.Tag> KEEP = EnumSet.of(
            POS.Tag.NNG, POS.Tag.NNP, POS.Tag.SL, POS.Tag.SN);

    @Test
    @DisplayName("품사 keep-list: 명사·영문·숫자만 남고 조사·어미·부사는 제거된다")
    void keepListFiltersPos() {
        KoreanAnalyzer analyzer = new KoreanAnalyzer(KEEP, null);

        List<String> tokens = analyzer.analyze("검색 엔진을 빠르게 개발합니다. PDF 파일 2026년");

        assertThat(tokens).contains("검색", "엔진", "개발", "파일", "2026");
        assertThat(tokens).contains("pdf");                    // SL + LowerCaseFilter
        assertThat(tokens).doesNotContain("을", "빠르게", "합니다", "PDF");
    }

    @Test
    @DisplayName("사용자 사전 복합명사: MIXED 모드로 원형과 분해형이 모두 색인된다")
    void userDictionaryCompound() throws IOException {
        UserDictionary dict = UserDictionary.open(new StringReader("검색엔진 검색 엔진"));
        KoreanAnalyzer analyzer = new KoreanAnalyzer(KEEP, dict);

        List<String> tokens = analyzer.analyze("검색엔진 도입 안내");

        assertThat(tokens).contains("검색엔진", "검색", "엔진", "도입", "안내");
    }

    @Test
    @DisplayName("reload: 사전 교체 후 신조어가 단일 토큰으로 분석된다")
    void reloadSwapsDictionary() throws IOException {
        KoreanAnalyzer analyzer = new KoreanAnalyzer(KEEP, null);
        List<String> before = analyzer.analyze("고넷검색 서비스");

        analyzer.reload(UserDictionary.open(new StringReader("고넷검색")));
        List<String> after = analyzer.analyze("고넷검색 서비스");

        assertThat(before).doesNotContain("고넷검색");
        assertThat(after).contains("고넷검색", "서비스");
    }

    @Test
    @DisplayName("빈 입력은 빈 토큰 리스트를 반환한다")
    void blankInput() {
        KoreanAnalyzer analyzer = new KoreanAnalyzer(KEEP, null);

        assertThat(analyzer.analyze(null)).isEmpty();
        assertThat(analyzer.analyze("   ")).isEmpty();
    }
}
