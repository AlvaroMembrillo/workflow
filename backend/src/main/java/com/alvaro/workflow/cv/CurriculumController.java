package com.alvaro.workflow.cv;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alvaro.workflow.auth.UsuarioActual;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/candidatos/me/cv")
@PreAuthorize("hasRole('CANDIDATO')")
@RequiredArgsConstructor
@Tag(name = "Currículum")
public class CurriculumController {

	private final CurriculumService curriculumService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Sube el currículum del candidato en PDF",
			description = "Máximo 5 MB. Si ya tenía uno, lo sustituye. Lo ven las empresas en cuyas ofertas se inscribe.")
	public CurriculumResponse subir(@UsuarioActual UUID usuarioId, @RequestPart("fichero") MultipartFile fichero) {
		return curriculumService.guardar(usuarioId, fichero);
	}

	@GetMapping
	@Operation(summary = "Nombre, tamaño y fecha del currículum del candidato", description = "404 si no ha subido ninguno.")
	public CurriculumResponse resumen(@UsuarioActual UUID usuarioId) {
		return curriculumService.resumenDe(usuarioId);
	}

	@GetMapping(path = "/fichero", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(summary = "Descarga el currículum del candidato")
	public ResponseEntity<byte[]> descargar(@UsuarioActual UUID usuarioId) {
		return DescargaDeCurriculum.de(curriculumService.obtener(usuarioId));
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Borra el currículum del candidato", description = "Las empresas dejan de poder descargarlo.")
	public void borrar(@UsuarioActual UUID usuarioId) {
		curriculumService.borrar(usuarioId);
	}

}
