package com.gonet.search.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청자 IP를 ClientIpHolder(ThreadLocal)에 보관한다.
 * X-Forwarded-For 첫 값 우선(프록시 환경), 없으면 remoteAddr.
 */
@Component
public class ClientIpFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            ClientIpHolder.set(extractClientIp(request));
            filterChain.doFilter(request, response);
        } finally {
            ClientIpHolder.clear();
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
