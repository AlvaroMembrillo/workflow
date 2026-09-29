package com.alvaro.workflow.empresa.dto;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmpresaRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
		String nombre,

		@Size(max = 5000, message = "La descripción no puede superar los 5000 caracteres")
		String descripcion,

		@URL(message = "El sitio web debe ser una URL válida")
		@Size(max = 255, message = "El sitio web no puede superar los 255 caracteres")
		String sitioWeb,

		@Size(max = 150, message = "La ubicación no puede superar los 150 caracteres")
		String ubicacion) {
}
