package com.sabortradicion.app.dto;

import java.time.Instant;

public record LoginResponse(String token, String username, Instant expiresAt) {
}
