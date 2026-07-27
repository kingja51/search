package com.gonet.search.repository;

import com.gonet.search.domain.SearchIndex;
import com.gonet.search.domain.SearchIndexId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchIndexRepository extends JpaRepository<SearchIndex, SearchIndexId> {

    long countByDocType(String docType);
}
