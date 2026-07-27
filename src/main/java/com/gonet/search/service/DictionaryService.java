package com.gonet.search.service;

import com.gonet.search.analyzer.KoreanAnalyzer;
import com.gonet.search.analyzer.UserDictionaryLoader;
import com.gonet.search.config.ClientIpHolder;
import com.gonet.search.domain.DicBanned;
import com.gonet.search.domain.DicSynonym;
import com.gonet.search.domain.DicWord;
import com.gonet.search.domain.RecommendKeyword;
import com.gonet.search.mapper.DicBannedMapper;
import com.gonet.search.mapper.DicSynonymMapper;
import com.gonet.search.mapper.DicWordMapper;
import com.gonet.search.mapper.RecommendKeywordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 사전 관리 (DESIGN.md 4.2 / 5.2 / 어드민).
 * CRUD 후 캐시 evict + Nori 사용자 사전 리로드는 **트랜잭션 커밋 후(AFTER_COMMIT)** 수행한다
 * — 커밋 전 evict 시 이전 데이터가 다시 캐시될 수 있기 때문 (DESIGN.md 5.2 주의).
 * ※ 단어사전 변경을 기존 색인에 반영하려면 전체 재색인(IndexingService.rebuildAll)이 추가로 필요.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DictionaryService {

    private final UserDictionaryLoader userDictionaryLoader;
    private final KoreanAnalyzer koreanAnalyzer;
    private final DicWordMapper dicWordMapper;
    private final DicSynonymMapper dicSynonymMapper;
    private final DicBannedMapper dicBannedMapper;
    private final RecommendKeywordMapper recommendKeywordMapper;
    private final CacheManager cacheManager;

    private static final List<String> EVICT_CACHES =
            List.of("synonyms", "bannedWords", "recommendKeywords", "autocomplete", "popularKeywords");

    /* ── 조회 (어드민 목록: 비활성 포함) ── */

    public List<DicWord> words() {
        return dicWordMapper.findAll();
    }

    public List<DicSynonym> synonyms() {
        return dicSynonymMapper.findAll();
    }

    public List<DicBanned> banned() {
        return dicBannedMapper.findAll();
    }

    public List<RecommendKeyword> recommends() {
        return recommendKeywordMapper.findAll();
    }

    /* ── 단어사전 ── */

    @Transactional
    public void addWord(DicWord word) {
        dicWordMapper.insert(word);
        reloadAfterCommit();
    }

    @Transactional
    public void toggleWord(Long id) {
        dicWordMapper.toggleEnabled(id, ClientIpHolder.get());
        reloadAfterCommit();
    }

    @Transactional
    public void deleteWord(Long id) {
        dicWordMapper.deleteById(id);
        reloadAfterCommit();
    }

    /* ── 동의어사전 ── */

    @Transactional
    public void addSynonym(DicSynonym synonym) {
        dicSynonymMapper.insert(synonym);
        reloadAfterCommit();
    }

    @Transactional
    public void toggleSynonym(Long id) {
        dicSynonymMapper.toggleEnabled(id, ClientIpHolder.get());
        reloadAfterCommit();
    }

    @Transactional
    public void deleteSynonym(Long id) {
        dicSynonymMapper.deleteById(id);
        reloadAfterCommit();
    }

    /* ── 금지어사전 ── */

    @Transactional
    public void addBanned(DicBanned banned) {
        dicBannedMapper.insert(banned);
        reloadAfterCommit();
    }

    @Transactional
    public void toggleBanned(Long id) {
        dicBannedMapper.toggleEnabled(id, ClientIpHolder.get());
        reloadAfterCommit();
    }

    @Transactional
    public void deleteBanned(Long id) {
        dicBannedMapper.deleteById(id);
        reloadAfterCommit();
    }

    /* ── 추천 검색어 ── */

    @Transactional
    public void addRecommend(RecommendKeyword keyword) {
        recommendKeywordMapper.insert(keyword);
        reloadAfterCommit();
    }

    @Transactional
    public void toggleRecommend(Long id) {
        recommendKeywordMapper.toggleEnabled(id, ClientIpHolder.get());
        reloadAfterCommit();
    }

    @Transactional
    public void deleteRecommend(Long id) {
        recommendKeywordMapper.deleteById(id);
        reloadAfterCommit();
    }

    /* ── 리로드 ── */

    /**
     * 캐시 evict + Nori 사용자 사전 교체 (재기동 불필요).
     * @CacheEvict 어노테이션 대신 CacheManager 직접 evict — afterCommit 콜백(자기 호출)에서는
     * 프록시가 우회되어 어노테이션이 동작하지 않기 때문.
     */
    public void reloadDictionaries() {
        for (String name : EVICT_CACHES) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
        koreanAnalyzer.reload(userDictionaryLoader.load());
        log.info("사전 리로드 완료: Nori 사용자 사전 교체 + 캐시 evict {}", EVICT_CACHES);
    }

    /** 트랜잭션 커밋 후 리로드 예약 (커밋 전 evict로 인한 이전 데이터 재캐시 방지) */
    private void reloadAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            reloadDictionaries();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                reloadDictionaries();
            }
        });
    }
}
