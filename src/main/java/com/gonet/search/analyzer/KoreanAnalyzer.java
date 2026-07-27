package com.gonet.search.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.KoreanPartOfSpeechStopFilter;
import org.apache.lucene.analysis.ko.KoreanTokenizer;
import org.apache.lucene.analysis.ko.POS;
import org.apache.lucene.analysis.ko.dict.UserDictionary;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Nori 형태소 분석 래퍼: 문자열 → 토큰 리스트 (DESIGN.md 4.2).
 *
 * - 품사 keep-list 방식: keepTags 외 품사는 전부 제거 (기본 NNG·NNP·SL·SN)
 * - 복합명사 MIXED 분해: 원형 + 분해형 모두 색인
 * - LowerCaseFilter 포함: 영문(SL) 대소문자 통일 (PDF → pdf)
 * - 색인과 검색은 반드시 이 인스턴스 하나를 공유한다 (토큰 불일치 방지)
 * - 사전 변경 시 reload()로 내부 Analyzer만 교체 (무재기동)
 */
public class KoreanAnalyzer {

    private final Set<POS.Tag> stopTags;
    private volatile Analyzer delegate;

    public KoreanAnalyzer(Set<POS.Tag> keepTags, UserDictionary userDictionary) {
        // Nori의 StopFilter는 "제거" 방식이므로 전체 품사에서 keep-list를 뺀 나머지를 제거 대상으로 만든다
        Set<POS.Tag> stops = EnumSet.allOf(POS.Tag.class);
        stops.removeAll(keepTags);
        this.stopTags = stops;
        this.delegate = build(userDictionary);
    }

    /** 문자열을 형태소 분석해 keep-list 품사 토큰만 반환한다. */
    public List<String> analyze(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        try (TokenStream stream = delegate.tokenStream("", text)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                String token = term.toString();
                if (!token.isBlank()) {
                    tokens.add(token);
                }
            }
            stream.end();
        } catch (IOException e) {
            throw new UncheckedIOException("형태소 분석 실패", e);
        }
        return tokens;
    }

    /**
     * 사용자 사전 변경 시 내부 Analyzer 교체 (volatile 스왑 — 재기동 불필요).
     * 구 인스턴스는 close하지 않고 GC에 맡긴다 — 교체 직전에 참조를 잡은 스레드가
     * close된 Analyzer로 tokenStream()을 호출하면 AlreadyClosedException으로 검색이 실패할 수 있다.
     * (리로드는 사전 변경 시에만 드물게 발생하므로 미회수 비용은 무시 가능)
     */
    public synchronized void reload(UserDictionary userDictionary) {
        this.delegate = build(userDictionary);
    }

    private Analyzer build(UserDictionary userDictionary) {
        Set<POS.Tag> stops = this.stopTags;
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                KoreanTokenizer tokenizer = new KoreanTokenizer(
                        TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY,
                        userDictionary,
                        KoreanTokenizer.DecompoundMode.MIXED,   // 복합명사: 원형+분해형 모두 색인
                        false);
                TokenStream stream = new KoreanPartOfSpeechStopFilter(tokenizer, stops);
                stream = new LowerCaseFilter(stream);
                return new TokenStreamComponents(tokenizer, stream);
            }
        };
    }
}
