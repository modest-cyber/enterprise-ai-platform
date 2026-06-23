package com.aiplatform.framework.web.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * 内部接口 Token 服务 — 为 FastAPI→Spring Boot 内部调用提供短期 JWT 认证
 *
 * @author aiplatform
 */
@Component
public class InternalTokenService {

    /** 内部 Token 有效期（分钟），默认 30 */
    @Value("${ai.internal.token-expire-time:30}")
    private int expireTime;

    /** JWT 签名密钥（与用户 Token 共用） */
    @Value("${token.secret}")
    private String secret;

    /** 预共享密钥（用于 /ai/internal/auth/token 端点的初始认证） */
    @Value("${ai.internal.secret:internal-shared-secret}")
    private String sharedSecret;

    private static final long MILLIS_MINUTE = 60 * 1000L;

    /**
     * 生成内部 JWT
     * subject = "internal"，有效期 30 分钟
     */
    public String generateToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(Claims.SUBJECT, "internal");
        claims.put("iat", System.currentTimeMillis());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireTime * MILLIS_MINUTE))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 验证内部 JWT 并返回 Claims
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证预共享密钥是否匹配
     */
    public boolean validateSharedSecret(String secret) {
        return sharedSecret.equals(secret);
    }
}
