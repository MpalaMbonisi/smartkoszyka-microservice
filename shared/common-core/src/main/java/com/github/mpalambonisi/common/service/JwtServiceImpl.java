package com.github.mpalambonisi.common.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/** Default JWT implementation that can be used by any microservice. */
@Service
public class JwtServiceImpl implements JwtService {

  @Value("${jwt.secret.key:default_secret_key_for_dev_only_1234567890}")
  private String secretKey;

  @Value("${jwt.expiration.ms:3600000}")
  private long jwtExpiration;

  @Override
  public String generateToken(UserDetails user) {
    return generateToken(new HashMap<>(), user);
  }

  /**
   * Builds a signed JWT with extra claims.
   *
   * @param extraClaims additional key/value pairs to embed in the payload
   * @param userDetails principal whose username becomes the subject
   * @return compact serialized JWT
   */
  public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .claims(extraClaims)
        .subject(userDetails.getUsername())
        .issuedAt(new Date(now))
        .expiration(new Date(now + jwtExpiration))
        .signWith(getSignInKey())
        .compact();
  }

  @Override
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  @Override
  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  @Override
  public boolean isTokenValid(String token, String username) {
    final String extractedUsername = extractUsername(token);
    return extractedUsername != null
        && extractedUsername.equals(username)
        && !isTokenExpired(token);
  }

  @Override
  public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
    final Claims claims = extractAllClaims(token);
    return claimResolver.apply(claims);
  }

  // ── private helpers ──────────────────────────────────────────────────────

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
  }

  private boolean isTokenExpired(String token) {
    return extractExpirationDate(token).before(new Date());
  }

  private Date extractExpirationDate(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private SecretKey getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
