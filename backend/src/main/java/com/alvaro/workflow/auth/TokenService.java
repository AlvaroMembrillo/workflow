package com.alvaro.workflow.auth;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.alvaro.workflow.auth.dto.TokenResponse;
import com.alvaro.workflow.config.JwtProperties;
import com.alvaro.workflow.usuario.Usuario;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

	/** Claim con los roles del usuario; {@code JwtConfig} lo convierte en authorities {@code ROLE_*}. */
	public static final String CLAIM_ROLES = "roles";

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public TokenResponse generar(Usuario usuario) {
		Instant ahora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtProperties.issuer())
				.issuedAt(ahora)
				.expiresAt(ahora.plus(jwtProperties.duracionToken()))
				.subject(usuario.getId().toString())
				.claim("email", usuario.getEmail())
				.claim(CLAIM_ROLES, List.of(usuario.getRol().name()))
				.build();

		String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
		return new TokenResponse(token, "Bearer", jwtProperties.duracionToken().toSeconds());
	}

}
