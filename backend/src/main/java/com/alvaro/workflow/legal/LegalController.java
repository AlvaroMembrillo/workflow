package com.alvaro.workflow.legal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.workflow.correo.CorreoProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/legal")
@RequiredArgsConstructor
@Tag(name = "Legal")
@SecurityRequirements
public class LegalController {

	private final LegalProperties legal;
	private final CorreoProperties correo;

	/** @param contacto dirección para ejercer los derechos de protección de datos y para cualquier consulta */
	public record Titular(String titular, String nif, String domicilio, String contacto) {
	}

	@GetMapping
	@Operation(summary = "Quién responde del portal, para las páginas legales de la web")
	public Titular titular() {
		return new Titular(legal.titular(), legal.nif(), legal.domicilio(), correo.contacto());
	}

}
