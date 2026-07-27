package com.gonet.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 스케줄링 활성화: 색인 동기화(매일 2회), 인기 검색어 MV 갱신(10분) 등 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
