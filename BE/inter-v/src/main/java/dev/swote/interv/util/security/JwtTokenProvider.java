package dev.swote.interv.util.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret:mySecretKeyForJwtTokenGenerationThatNeedsToBeAtLeast256BitsLong}") String secret,
            @Value("${jwt.access-token-validity:3600000}") long accessTokenValidityInMilliseconds,
            @Value("${jwt.refresh-token-validity:604800000}") long refreshTokenValidityInMilliseconds) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMilliseconds = accessTokenValidityInMilliseconds;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;

        log.info("JWT 토큰 제공자 초기화 완료 - 액세스 토큰 유효기간: {}ms, 리프레시 토큰 유효기간: {}ms",
                accessTokenValidityInMilliseconds, refreshTokenValidityInMilliseconds);
    }

    public String createAccessToken(String email, Integer userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        try {
            String token = Jwts.builder()
                    .setSubject(email)
                    .claim("userId", userId)
                    .claim("type", "access")
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(secretKey, SignatureAlgorithm.HS256)
                    .compact();

            log.debug("액세스 토큰 생성 성공 - 이메일: {}, 사용자 ID: {}", email, userId);
            return token;
        } catch (Exception e) {
            log.error("액세스 토큰 생성 실패 - 이메일: {}, 사용자 ID: {}, 오류: {}", email, userId, e.getMessage());
            throw new RuntimeException("액세스 토큰 생성에 실패했습니다", e);
        }
    }

    public String createRefreshToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        try {
            String token = Jwts.builder()
                    .setSubject(email)
                    .claim("type", "refresh")
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(secretKey, SignatureAlgorithm.HS256)
                    .compact();

            log.debug("리프레시 토큰 생성 성공 - 이메일: {}", email);
            return token;
        } catch (Exception e) {
            log.error("리프레시 토큰 생성 실패 - 이메일: {}, 오류: {}", email, e.getMessage());
            throw new RuntimeException("리프레시 토큰 생성에 실패했습니다", e);
        }
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.debug("토큰이 비어있음");
            return false;
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 토큰: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT 서명이 유효하지 않음: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못됨: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 예상치 못한 오류: {}", e.getMessage());
        }
        return false;
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = parseClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("토큰에서 이메일 추출 실패: {}", e.getMessage());
            throw new RuntimeException("토큰에서 이메일을 추출할 수 없습니다", e);
        }
    }

    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = parseClaimsFromToken(token);
            return claims.get("userId", Integer.class);
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            throw new RuntimeException("토큰에서 사용자 ID를 추출할 수 없습니다", e);
        }
    }

    public String getTokenTypeFromToken(String token) {
        try {
            Claims claims = parseClaimsFromToken(token);
            return claims.get("type", String.class);
        } catch (Exception e) {
            log.error("토큰에서 타입 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = parseClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("토큰에서 만료일 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            log.error("토큰 만료 확인 실패: {}", e.getMessage());
            return true; // 오류 발생 시 만료된 것으로 간주
        }
    }

    public long getAccessTokenValidityInSeconds() {
        return accessTokenValidityInMilliseconds / 1000;
    }

    public long getRefreshTokenValidityInSeconds() {
        return refreshTokenValidityInMilliseconds / 1000;
    }

    /**
     * 토큰에서 Claims 파싱 (공통 메서드)
     */
    private Claims parseClaimsFromToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("토큰이 비어있습니다");
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("만료된 토큰에서 Claims 추출: {}", e.getMessage());
            return e.getClaims(); // 만료되어도 Claims는 추출 가능
        } catch (Exception e) {
            log.error("토큰 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("토큰을 파싱할 수 없습니다", e);
        }
    }

    /**
     * 토큰 정보 로깅 (디버깅용)
     */
    public void logTokenInfo(String token) {
        if (log.isDebugEnabled()) {
            try {
                Claims claims = parseClaimsFromToken(token);
                log.debug("토큰 정보 - 이메일: {}, 사용자 ID: {}, 타입: {}, 발급일: {}, 만료일: {}",
                        claims.getSubject(),
                        claims.get("userId"),
                        claims.get("type"),
                        claims.getIssuedAt(),
                        claims.getExpiration());
            } catch (Exception e) {
                log.debug("토큰 정보 로깅 실패: {}", e.getMessage());
            }
        }
    }
}