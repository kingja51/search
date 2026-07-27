package com.gonet.search.repository;

import com.gonet.search.domain.SearchKeywordLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchKeywordLogRepository extends JpaRepository<SearchKeywordLog, Long> {
}
