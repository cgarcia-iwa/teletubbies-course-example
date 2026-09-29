package com.teletubbies.course.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final String ROLE_CLAIM = "role";
  private static final String ROLE_PREFIX = "ROLE_";

  private final SecretKey signingKey;
  private final JwtProperties properties;

  public JwtService(final JwtProperties properties) {
    this.properties = properties;
    // Throws WeakKeyException at startup if the secret is shorter than 256 bits.
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
  }

  public String generateToken(final UserDetails user) {
    final Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getUsername())
        .claim(ROLE_CLAIM, extractRoleName(user))
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(properties.expiration())))
        .signWith(signingKey)
        .compact();
  }

  /** Throws {@link io.jsonwebtoken.JwtException} if the token is expired, tampered or malformed. */
  public String extractUsername(final String token) {
    return parseClaims(token).getSubject();
  }

  public String extractRole(final String token) {
    return parseClaims(token).get(ROLE_CLAIM, String.class);
  }

  public boolean isTokenValid(final String token, final UserDetails user) {
    // Signature and expiration are already validated by parseClaims.
    return extractUsername(token).equals(user.getUsername()) && user.isEnabled();
  }

  public long getExpirationSeconds() {
    return properties.expiration().toSeconds();
  }

  private Claims parseClaims(final String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  // "ROLE_TEACHER" → "TEACHER"
  private String extractRoleName(final UserDetails user) {
    return user.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith(ROLE_PREFIX))
        .map(authority -> authority.substring(ROLE_PREFIX.length()))
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("User has no role: " + user.getUsername()));
  }
}