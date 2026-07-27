package com.gonet.search.config;

import com.gonet.search.domain.BaseEntity;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * MyBatis 감사 인터셉터 (DESIGN.md 4.6).
 * INSERT: created_at/ip/by + updated_at 주입, UPDATE: updated_at/ip/by 주입.
 * 파라미터가 BaseEntity(단건/Map/Collection 내부 포함)일 때 동작한다.
 * 생성자/수정자 ID: 웹 요청 = guest, 배치·스케줄러 = system (Spring Security 도입 시 이 로직만 교체)
 */
@Intercepts(@Signature(type = Executor.class, method = "update",
        args = {MappedStatement.class, Object.class}))
@Component
public class AuditInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        applyAudit(parameter, ms.getSqlCommandType());
        return invocation.proceed();
    }

    private void applyAudit(Object parameter, SqlCommandType commandType) {
        if (parameter == null) {
            return;
        }
        if (parameter instanceof BaseEntity entity) {
            fill(entity, commandType);
        } else if (parameter instanceof Map<?, ?> map) {
            map.values().forEach(v -> applyAudit(v, commandType));
        } else if (parameter instanceof Collection<?> collection) {
            collection.forEach(v -> applyAudit(v, commandType));
        }
    }

    private void fill(BaseEntity entity, SqlCommandType commandType) {
        OffsetDateTime now = OffsetDateTime.now();
        String ip = ClientIpHolder.get();
        String by = currentAuditor();
        if (commandType == SqlCommandType.INSERT) {
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(now);
            }
            entity.setCreatedIp(ip);
            entity.setCreatedBy(by);
            entity.setUpdatedAt(now);
        } else if (commandType == SqlCommandType.UPDATE) {
            entity.setUpdatedAt(now);
            entity.setUpdatedIp(ip);
            entity.setUpdatedBy(by);
        }
    }

    private String currentAuditor() {
        return RequestContextHolder.getRequestAttributes() != null ? "guest" : "system";
    }
}
