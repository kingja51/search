package com.gonet.search.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 색인 작업 공유 락 (DESIGN.md 4.4).
 * 색인 동기화·전체 재색인·파일 추출이 하나의 락을 공유해
 * 동시 실행 시 한쪽이 다른 쪽 반영분을 덮어쓰는 레이스를 방지한다.
 * ReentrantLock이므로 파일 추출이 마지막에 색인 동기화를 이어서 호출해도(같은 스레드) 통과한다.
 */
@Component
public class IndexJobLock {

    private final ReentrantLock lock = new ReentrantLock();

    /** 락 획득 시도 — 다른 색인 작업이 실행 중이면 false (대기하지 않음) */
    public boolean tryLock() {
        return lock.tryLock();
    }

    public void unlock() {
        lock.unlock();
    }
}
