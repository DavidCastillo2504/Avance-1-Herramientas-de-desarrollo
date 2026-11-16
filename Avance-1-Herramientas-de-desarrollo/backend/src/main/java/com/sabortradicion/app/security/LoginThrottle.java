package com.sabortradicion.app.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bloqueo simple en memoria: 5 intentos fallidos desde una IP bloquean el inicio de sesión 15 minutos. */
@Component
public class LoginThrottle {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private record Attempts(int failures, Instant firstFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public LoginThrottle() {
        this(Clock.systemUTC());
    }

    LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        Attempts current = attempts.get(key);
        if (current == null) return false;
        if (expired(current)) {
            attempts.remove(key);
            return false;
        }
        return current.failures() >= MAX_FAILURES;
    }

    public void registerFailure(String key) {
        if (attempts.size() > 10_000) {
            attempts.values().removeIf(this::expired);
        }
        attempts.compute(key, (k, current) -> current == null || expired(current)
                ? new Attempts(1, clock.instant())
                : new Attempts(current.failures() + 1, current.firstFailure()));
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private boolean expired(Attempts value) {
        return value.firstFailure().plus(WINDOW).isBefore(clock.instant());
    }
}
