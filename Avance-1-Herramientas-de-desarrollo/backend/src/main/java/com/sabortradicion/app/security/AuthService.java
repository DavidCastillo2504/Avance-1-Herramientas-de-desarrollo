package com.sabortradicion.app.security;

import com.sabortradicion.app.dto.LoginResponse;
import com.sabortradicion.app.exception.InvalidCredentialsException;
import com.sabortradicion.app.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Autenticación del único usuario administrador. Las credenciales llegan por variables
 * de entorno y la contraseña se conserva en memoria solo como hash BCrypt.
 */
@Service
public class AuthService {
    private final String adminUsername;
    private final String adminPasswordHash;
    private final Duration tokenDuration;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final LoginThrottle throttle;
    private final Clock clock;

    @Autowired
    public AuthService(@Value("${app.security.admin-username}") String adminUsername,
                       @Value("${app.security.admin-password}") String adminPassword,
                       @Value("${app.security.token-hours}") long tokenHours,
                       PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder, LoginThrottle throttle) {
        this(adminUsername, adminPassword, tokenHours, passwordEncoder, jwtEncoder, throttle, Clock.systemUTC());
    }

    AuthService(String adminUsername, String adminPassword, long tokenHours, PasswordEncoder passwordEncoder,
                JwtEncoder jwtEncoder, LoginThrottle throttle, Clock clock) {
        if (adminUsername == null || adminUsername.isBlank()) {
            throw new IllegalStateException("ADMIN_USERNAME no puede estar vacío");
        }
        if (adminPassword == null || adminPassword.length() < 10) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 10 caracteres");
        }
        this.adminUsername = adminUsername.trim();
        this.adminPasswordHash = passwordEncoder.encode(adminPassword);
        this.tokenDuration = Duration.ofHours(Math.max(1, tokenHours));
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.throttle = throttle;
        this.clock = clock;
    }

    public LoginResponse login(String username, String password, String clientKey) {
        if (throttle.isBlocked(clientKey)) {
            throw new TooManyRequestsException("Demasiados intentos fallidos. Intenta nuevamente en unos minutos.");
        }
        // BCrypt se evalúa siempre para no revelar por tiempo de respuesta si el usuario es correcto.
        boolean passwordMatches = passwordEncoder.matches(password, adminPasswordHash);
        if (!adminUsername.equals(username.trim()) || !passwordMatches) {
            throttle.registerFailure(clientKey);
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }
        throttle.reset(clientKey);

        Instant now = clock.instant();
        Instant expiresAt = now.plus(tokenDuration);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sabor-api")
                .subject(adminUsername)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("scope", "admin")
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new LoginResponse(token, adminUsername, expiresAt);
    }
}
