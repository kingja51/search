package com.gonet.search.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 검색 대상(doc_type) 정의의 단일 소스 — application.yml `search.doc-types` 맵.
 * 맵의 키 = doc_type 코드, 값 = 화면 라벨, **순서 = 통합검색 그룹·좌측 메뉴·select 노출 순서**.
 *
 * 새 검색 VIEW를 추가할 때는 yml에 한 줄만 추가하면
 * 검색 대상 select / 좌측 카테고리 / 그룹 제목 / 색인 카드·게이지 / 통계 라벨에 모두 반영된다.
 * (docs/add-search-source.md 참조)
 */
@Component
@ConfigurationProperties(prefix = "search")
@Getter
@Setter
public class SearchDocTypes {

    private Map<String, String> docTypes = new LinkedHashMap<>();

    /** doc_type 코드 목록 (yml 정의 순서) */
    public List<String> codes() {
        return List.copyOf(docTypes.keySet());
    }

    /** 화면 라벨 — 미정의 코드는 코드값 그대로 반환 */
    public String label(String code) {
        if (code == null) {
            return "";
        }
        return docTypes.getOrDefault(code, code);
    }
}
