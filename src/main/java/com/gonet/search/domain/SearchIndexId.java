package com.gonet.search.domain;

import java.io.Serializable;
import java.util.Objects;

/** tn_search_index 복합 PK (doc_type, doc_id) */
public class SearchIndexId implements Serializable {

    private String docType;
    private Long docId;

    public SearchIndexId() {
    }

    public SearchIndexId(String docType, Long docId) {
        this.docType = docType;
        this.docId = docId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchIndexId that)) return false;
        return Objects.equals(docType, that.docType) && Objects.equals(docId, that.docId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docType, docId);
    }
}
