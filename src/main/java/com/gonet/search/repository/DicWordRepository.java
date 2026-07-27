package com.gonet.search.repository;

import com.gonet.search.domain.DicWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DicWordRepository extends JpaRepository<DicWord, Long> {

    List<DicWord> findAllByEnabledTrue();
}
