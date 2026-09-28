package com.alvaro.workflow.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import org.springframework.security.converter.RsaKeyConverters;

import lombok.extern.slf4j.Slf4j;

/**
 * Carga el par de claves RSA con el que se firman los JWT. Solo se guarda la clave privada:
 * la pública se obtiene de ella, así no hay dos ficheros que mantener sincronizados.
 */
@Slf4j
final class ClavesRsa {

	private static final int TAMANO_CLAVE = 2048;

	private ClavesRsa() {
	}

	/**
	 * Lee la clave de {@code ruta}. Si no existe y se permite, genera una nueva y la guarda ahí
	 * para que los tokens sigan siendo válidos al reiniciar la aplicación.
	 */
	static KeyPair cargarOGenerar(Path ruta, boolean generarSiNoExiste) {
		if (Files.exists(ruta)) {
			return cargar(ruta);
		}
		if (!generarSiNoExiste) {
			throw new IllegalStateException("No existe la clave privada JWT en " + ruta.toAbsolutePath()
					+ ". Indica su ubicación con la variable de entorno JWT_CLAVE_PRIVADA");
		}

		KeyPair claves = generar();
		guardar(claves.getPrivate(), ruta);
		log.warn("Generada una clave privada JWT nueva en {}. Úsala solo en desarrollo", ruta.toAbsolutePath());
		return claves;
	}

	static KeyPair cargar(Path ruta) {
		try (InputStream entrada = Files.newInputStream(ruta)) {
			RSAPrivateKey privada = RsaKeyConverters.pkcs8().convert(entrada);
			return new KeyPair(clavePublica(privada), privada);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("No se pudo leer la clave privada JWT de " + ruta.toAbsolutePath(), ex);
		}
	}

	private static RSAPublicKey clavePublica(RSAPrivateKey privada) {
		if (!(privada instanceof RSAPrivateCrtKey crt)) {
			throw new IllegalStateException("La clave privada JWT no incluye el exponente público");
		}
		try {
			return (RSAPublicKey) KeyFactory.getInstance("RSA")
					.generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("No se pudo obtener la clave pública JWT", ex);
		}
	}

	private static KeyPair generar() {
		try {
			KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
			generador.initialize(TAMANO_CLAVE);
			return generador.generateKeyPair();
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("No se pudo generar la clave JWT", ex);
		}
	}

	private static void guardar(PrivateKey clave, Path ruta) {
		String pem = "-----BEGIN PRIVATE KEY-----\n"
				+ Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(clave.getEncoded())
				+ "\n-----END PRIVATE KEY-----\n";
		try {
			Path directorio = ruta.toAbsolutePath().getParent();
			if (directorio != null) {
				Files.createDirectories(directorio);
			}
			Files.writeString(ruta, pem, StandardCharsets.US_ASCII);
			// Que solo el usuario actual pueda leerla (en sistemas que lo permiten)
			if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
				Files.setPosixFilePermissions(ruta, PosixFilePermissions.fromString("rw-------"));
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("No se pudo guardar la clave privada JWT en " + ruta.toAbsolutePath(), ex);
		}
	}

}
