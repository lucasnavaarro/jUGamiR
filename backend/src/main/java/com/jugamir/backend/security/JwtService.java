package com.jugamir.backend.security;

import com.jugamir.backend.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateToken(String email, int tokenVersion, String rol) {
        return Jwts.builder()
                .subject(email)
                .claim("tokenVersion", tokenVersion)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getKey())
                .compact();

    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getRolFromToken(String token) {
        return getClaims(token).get("rol", String.class);
    }

    // Extraer versión de token sin consultar bd
    public int getTokenVersion(String token) {
        return getClaims(token).get("tokenVersion", Integer.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey()) // Obtiene la llave para verificar la firma
                .build()
                .parseSignedClaims(token) // Verifica la firma del token
                // Una vez verificado, obtenemos el payload
                .getPayload();
    }

    public void isTokenValid(String token) {
        try {
            getEmailFromToken(token);
        } catch (ExpiredJwtException e) {
            throw new BusinessException("El token ha expirado");
        } catch (MalformedJwtException e) {
            throw new BusinessException("El token tiene formato incorrecto");
        } catch (SignatureException e) {
            throw new BusinessException("La firma del token no es válida");
        } catch (JwtException e) {
            throw new BusinessException("Token inválido");
        }
    }
}
