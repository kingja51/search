package com.gonet.search.service;

import com.gonet.search.analyzer.KoreanAnalyzer;
import com.gonet.search.analyzer.UserDictionaryLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * 사전 반영 오케스트레이션 (DESIGN.md 4.2 / 5.2).
 * SQL로 사전(단어/동의어/금지어/추천어)을 변경한 뒤 이 메서드를 호출하면
 * 관련 캐시 evict + Nori 사용자 사전 리로드가 한 번에 수행된다 (재기동 불필요).
 *
 * ※ 어드민 CRUD 도입 시(추후): 트랜잭션 커밋 후(AFTER_COMMIT 이벤트)에 이 메서드를 호출할 것
 *   — 커밋 전 evict 시 이전 데이터가 다시 캐시될 수 있다 (DESIGN.md 5.2 주의).
 * ※ 단어사전 변경을 기존 색인에 반영하려면 IndexingService.rebuildAll()(전체 재색인)이 추가로 필요.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DictionaryService {

    private final UserDictionaryLoader userDictionaryLoader;
    private final KoreanAnalyzer koreanAnalyzer;

    @CacheEvict(cacheNames = {"synonyms", "bannedWords", "recommendKeywords", "autocomplete"},
            allEntries = true)
    public void reloadDictionaries() {
        koreanAnalyzer.reload(userDictionaryLoader.load());
        log.info("사전 리로드 완료: Nori 사용자 사전 교체 + 캐시 evict (synonyms/bannedWords/recommendKeywords/autocomplete)");
    }
}
