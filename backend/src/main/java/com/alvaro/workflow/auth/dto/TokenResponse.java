package com.alvaro.workflow.auth.dto;

/**
 * @param expiresIn segundos de validez del token desde que se emite
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
