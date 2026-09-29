package com.alvaro.workflow.config;

import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// API sin estado: el token viaja en la cabecera Authorization y no en cookies,
				// así que no hay sesión que proteger frente a CSRF
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(peticiones -> peticiones
						.requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
						// Lectura pública de ofertas y perfiles de empresa. /me va antes porque también encaja con /{id}
						.requestMatchers(HttpMethod.GET, "/api/empresas/me", "/api/empresas/me/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/ofertas", "/api/ofertas/{id}", "/api/empresas/{id}").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/actuator/health/**", "/error").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(recursos -> recursos.jwt(Customizer.withDefaults()));
		return http.build();
	}

	/** Usa BCrypt y guarda el algoritmo junto al hash ({bcrypt}...) para poder migrar a otro en el futuro. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	/** Solo lo usa el login para comprobar email y contraseña. */
	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(corsProperties.origenesPermitidos());
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		cors.setMaxAge(Duration.ofHours(1));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", cors);
		return source;
	}

}
