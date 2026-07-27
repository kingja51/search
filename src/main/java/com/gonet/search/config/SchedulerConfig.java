package com.gonet.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링·비동기 활성화.
 * 스케줄: 색인 동기화(매일 2회), 인기 검색어 MV 갱신(10분) / 비동기: 검색 로그 적재(@Async)
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulerConfig {
}
