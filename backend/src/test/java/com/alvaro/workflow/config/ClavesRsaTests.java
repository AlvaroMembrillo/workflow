package com.alvaro.workflow.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class ClavesRsaTests {

	@TempDir
	Path directorio;

	@Test
	void generaLaClaveSiNoExisteYLaReutilizaEnLosSiguientesArranques() {
		Path ruta = directorio.resolve("jwt/clave.pem");

		KeyPair generada = ClavesRsa.cargarOGenerar(ruta, true);
		KeyPair cargada = ClavesRsa.cargarOGenerar(ruta, false);

		assertThat(ruta).exists();
		assertThat(cargada.getPrivate()).isEqualTo(generada.getPrivate());
		assertThat(cargada.getPublic()).isEqualTo(generada.getPublic());
	}

	@Test
	void fallaSiLaClaveNoExisteYNoSePermiteGenerarla() {
		Path ruta = directorio.resolve("no-existe.pem");

		assertThatThrownBy(() -> ClavesRsa.cargarOGenerar(ruta, false))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_CLAVE_PRIVADA");
		assertThat(ruta).doesNotExist();
	}

	@Test
	@EnabledOnOs({ OS.LINUX, OS.MAC })
	void laClaveGeneradaSoloLaPuedeLeerElUsuarioActual() throws Exception {
		Path ruta = directorio.resolve("clave.pem");

		ClavesRsa.cargarOGenerar(ruta, true);

		assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(ruta))).isEqualTo("rw-------");
	}

}
