package com.gonet.search.repository;

import com.gonet.search.domain.DicBanned;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DicBannedRepository extends JpaRepository<DicBanned, Long> {

    List<DicBanned> findAllByEnabledTrue();
}
