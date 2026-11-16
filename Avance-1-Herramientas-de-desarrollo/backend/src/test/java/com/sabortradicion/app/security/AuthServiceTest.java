package com.sabortradicion.app.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.sabortradicion.app.exception.InvalidCredentialsException;
import com.sabortradicion.app.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {
    private static final String PASSWORD = "una-clave-segura-123";

    private final SecretKey key = new SecretKeySpec(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService("admin", PASSWORD, 8, new BCryptPasswordEncoder(4),
                new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key)), new LoginThrottle(), Clock.systemUTC());
    }

    @Test
    void issuesAnAdminTokenForValidCredentials() {
        var response = service.login("admin", PASSWORD, "1.1.1.1");

        Jwt jwt = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build().decode(response.token());
        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.getClaimAsString("scope")).isEqualTo("admin");
        assertThat(response.expiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void rejectsWrongPasswords() {
        assertThatThrownBy(() -> service.login("admin", "otra-clave-cualquiera", "1.1.1.1"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void blocksAClientAfterRepeatedFailures() {
        for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
            assertThatThrownBy(() -> service.login("admin", "mala", "2.2.2.2"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> service.login("admin", PASSWORD, "2.2.2.2"))
                .isInstanceOf(TooManyRequestsException.class);
        assertThat(service.login("admin", PASSWORD, "3.3.3.3").token()).isNotBlank();
    }

    @Test
    void refusesToStartWithAWeakPassword() {
        assertThatThrownBy(() -> new AuthService("admin", "corta", 8, new BCryptPasswordEncoder(4),
                new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key)), new LoginThrottle(), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
