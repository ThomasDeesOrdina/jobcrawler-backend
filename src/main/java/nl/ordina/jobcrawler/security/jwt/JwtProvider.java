package nl.ordina.jobcrawler.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import nl.ordina.jobcrawler.security.services.UserPrinciple;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class JwtProvider {

    @Value("${jwt.token}") //jwt.token in application.properties
    private String jwtSecret;

    @Value("${jwt.expire}") // jwt.expire in application.properties
    private int jwtExpiration;

    /**
     * Generates JWT Token based on user credentials
     * @param authentication authentication based on user credentials
     * @return List including jwt Token and jwt expiration information
     */
    public List<String> generateJwtToken(Authentication authentication) {
        UserPrinciple userPrincipal = (UserPrinciple) authentication.getPrincipal();

        List<String> jwtToken = new ArrayList<>();
        jwtToken.add(Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration * 1000))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact());
        jwtToken.add(String.valueOf(jwtExpiration));

        return jwtToken;
    }

    /**
     * Retrieve username based on jwt token
     * @param token token username is needed for
     * @return username
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    /**
     * Validates validity of token
     * @param authToken token
     * @return if token is valid
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature -> Message: {} ", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token -> Message: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token -> Message: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token -> Message: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty -> Message: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Refresh jwt token based on current (valid) token
     * @param authToken current working token
     * @return new token
     */
    public List<String> refreshToken(String authToken) {
        validateJwtToken(authToken);
        Optional<Jws<Claims>> claimsJws = getClaims(Optional.of(authToken));
        if (claimsJws.isEmpty()) {
            throw new AuthorizationServiceException("Invalid token claims");
        }
        Claims claims = claimsJws.get().getBody();
        claims.setIssuedAt(new Date());
        claims.setExpiration(new Date(System.currentTimeMillis() + jwtExpiration * 1000));
        List<String> refreshToken = new ArrayList<>();
        refreshToken.add(Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, jwtSecret).compact());
        refreshToken.add(String.valueOf(jwtExpiration));

        return refreshToken;
    }

    private Optional<Jws<Claims>> getClaims(Optional<String> authToken) {
        if (authToken.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken.get()));
    }
}
