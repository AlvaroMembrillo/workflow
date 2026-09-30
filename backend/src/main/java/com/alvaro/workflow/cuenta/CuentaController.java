package com.alvaro.workflow.cuenta;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.workflow.auth.CookieDeSesion;
import com.alvaro.workflow.auth.UsuarioActual;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios/me")
@RequiredArgsConstructor
@Tag(name = "Usuarios")
public class CuentaController {

	private final ExportacionDeDatos exportacion;
	private final BorradoDeCuentas borrado;
	private final CookieDeSesion cookieDeSesion;

	@GetMapping("/datos")
	@Operation(summary = "Descarga una copia de todos los datos personales del usuario, en JSON")
	public ResponseEntity<MisDatos> misDatos(@UsuarioActual UUID usuarioId) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore().cachePrivate())
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("workflow-mis-datos.json").build().toString())
				.body(exportacion.de(usuarioId));
	}

	@PostMapping("/baja")
	@Operation(summary = "Borra la cuenta del usuario y todos sus datos",
			description = "Hay que confirmar con la contraseña. No se puede deshacer. Si es una empresa, se borran "
					+ "sus ofertas y las candidaturas recibidas, y se avisa a quien esperaba respuesta.")
	public ResponseEntity<Void> darseDeBaja(@UsuarioActual UUID usuarioId, @Valid @RequestBody BajaRequest request) {
		borrado.borrarLaPropia(usuarioId, request.password());
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, cookieDeSesion.borrada().toString())
				.build();
	}

}
