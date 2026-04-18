package com.musicreviews.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiracionMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expiracionMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(String email, String rol) {
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiracionMs))
                .signWith(key)
                .compact();
    }

    public String extraerEmail(String token) {
        return parsear(token).getSubject();
    }

    public String extraerRol(String token) {
        return parsear(token).get("rol", String.class);
    }

    public boolean esValido(String token) {
        try {
            parsear(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parsear(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
