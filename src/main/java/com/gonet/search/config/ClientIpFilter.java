package com.gonet.search.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청자 IP를 ClientIpHolder(ThreadLocal)에 보관한다.
 *
 * X-Forwarded-For는 기본적으로 신뢰하지 않는다(search.trust-forwarded-header=false) —
 * 직접 노출 환경에서 XFF를 신뢰하면 클라이언트가 임의 헤더로 IP를 위조해
 * "내가 찾은 검색어" 조회·감사 컬럼(created_ip)을 오염시킬 수 있다.
 * 리버스 프록시 뒤에 배포할 때만 true로 켠다.
 */
@Component
public class ClientIpFilter extends OncePerRequestFilter {

    private final boolean trustForwardedHeader;

    public ClientIpFilter(@Value("${search.trust-forwarded-header:false}") boolean trustForwardedHeader) {
        this.trustForwardedHeader = trustForwardedHeader;
    }

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
        if (trustForwardedHeader) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
