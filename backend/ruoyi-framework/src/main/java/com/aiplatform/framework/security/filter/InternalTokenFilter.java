package com.aiplatform.framework.security.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aiplatform.common.utils.StringUtils;
import com.aiplatform.framework.web.service.InternalTokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 内部接口 Token 认证过滤器
 * <p>
 * 拦截 /ai/internal/** 路径：
 * - /ai/internal/auth/token：验证 X-Internal-Secret 预共享密钥
 * - 其他内部接口：验证 X-Internal-Token (内部 JWT)
 *
 * @author aiplatform
 */
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/ai/internal";
    private static final String AUTH_TOKEN_PATH = "/ai/internal/auth/token";
    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    private static final String HEADER_INTERNAL_SECRET = "X-Internal-Secret";

    @Autowired
    private InternalTokenService internalTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 非内部接口路径，跳过
        if (!path.startsWith(INTERNAL_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        // /ai/internal/auth/token — 使用预共享密钥认证
        if (path.equals(AUTH_TOKEN_PATH)) {
            String sharedSecret = request.getHeader(HEADER_INTERNAL_SECRET);
            if (StringUtils.isEmpty(sharedSecret)
                    || !internalTokenService.validateSharedSecret(sharedSecret)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"Internal secret invalid\"}");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 其他内部接口 — 验证内部 JWT
        String token = request.getHeader(HEADER_INTERNAL_TOKEN);
        if (StringUtils.isEmpty(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Missing internal token\"}");
            return;
        }

        try {
            Claims claims = internalTokenService.validateToken(token);
            String subject = claims.getSubject();
            if (!"internal".equals(subject)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"Invalid internal token subject\"}");
                return;
            }
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Internal token expired\"}");
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Invalid internal token\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
