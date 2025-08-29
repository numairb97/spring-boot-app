package io.spring.infrastructure.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultJwtService implements JwtService {
  private final SecretKey signingKey;
  private int sessionTime;

  @Autowired
  public DefaultJwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.sessionTime}") int sessionTime) {
    this.sessionTime = sessionTime;
    this.signingKey = createSecretKey(secret);
  }

  private SecretKey createSecretKey(String secret) {
    try {
      byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
      if (secretBytes.length < 64) {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(secretBytes);
        byte[] expandedKey = new byte[64];
        for (int i = 0; i < 64; i++) {
          expandedKey[i] = hash[i % hash.length];
        }
        secretBytes = expandedKey;
      }
      return Keys.hmacShaKeyFor(secretBytes);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create JWT signing key", e);
    }
  }

  @Override
  public String toToken(User user) {
    return Jwts.builder()
        .subject(user.getId())
        .expiration(expireTimeFromNow())
        .signWith(signingKey)
        .compact();
  }

  @Override
  public Optional<String> getSubFromToken(String token) {
    try {
      Jws<Claims> claimsJws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
      return Optional.ofNullable(claimsJws.getPayload().getSubject());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private Date expireTimeFromNow() {
    return new Date(System.currentTimeMillis() + sessionTime * 1000L);
  }
}
