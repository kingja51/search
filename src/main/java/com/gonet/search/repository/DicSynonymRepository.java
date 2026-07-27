package com.gonet.search.repository;

import com.gonet.search.domain.DicSynonym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DicSynonymRepository extends JpaRepository<DicSynonym, Long> {

    List<DicSynonym> findAllByEnabledTrue();
}
