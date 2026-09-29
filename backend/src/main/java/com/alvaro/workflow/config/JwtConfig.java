package com.alvaro.workflow.config;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import com.alvaro.workflow.auth.TokenService;

/**
 * La aplicación emite y valida sus propios JWT, firmados con RS256.
 */
@Configuration
class JwtConfig {

	@Bean
	KeyPair clavesJwt(JwtProperties jwtProperties) {
		return ClavesRsa.cargarOGenerar(jwtProperties.clavePrivada(), jwtProperties.generarClaveSiNoExiste());
	}

	@Bean
	JwtEncoder jwtEncoder(KeyPair clavesJwt) {
		return NimbusJwtEncoder
				.withKeyPair((RSAPublicKey) clavesJwt.getPublic(), (RSAPrivateKey) clavesJwt.getPrivate())
				.build();
	}

	@Bean
	JwtDecoder jwtDecoder(KeyPair clavesJwt, JwtProperties jwtProperties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) clavesJwt.getPublic()).build();
		// Además de la firma y la caducidad, comprueba que el token lo ha emitido esta aplicación
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));
		return decoder;
	}

	/** Convierte el claim {@code roles} en authorities {@code ROLE_*} para usar {@code hasRole(...)}. */
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(TokenService.CLAIM_ROLES);
		authorities.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

}
