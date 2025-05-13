package com.project.socialmedia.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${socialmedia.app.secret}")
    private String appSecret;

    @Value("${socialmedia.expires.in}")
    private long expiresIn;

    // Signing key üretimi
    /*
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(appSecret.getBytes(StandardCharsets.UTF_8));
    }
    */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(appSecret);   // 256-bit Base64 anahtar
        return Keys.hmacShaKeyFor(keyBytes);                   // HS-256 anahtar üret
    }

    // Kullanıcı girişinden token üretme
    public String generateJwtToken(Authentication auth) {
        JwtUserDetails userDetails = (JwtUserDetails) auth.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiresIn);

        return Jwts.builder()
                .subject(String.valueOf(userDetails.getId()))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // UserId'den token üretme
    public String generateJwtTokenByUserId(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiresIn);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // Token'dan kullanıcı ID'sini alma
    public Long getUserIdFromJwt(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    // Token geçerli mi kontrol etme
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expiration.before(new Date());
    }
}