package com.gonet.search.repository;

import com.gonet.search.domain.Content;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, Long> {
}
