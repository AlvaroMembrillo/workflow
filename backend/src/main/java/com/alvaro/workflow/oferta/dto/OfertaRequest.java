package com.alvaro.workflow.oferta.dto;

import com.alvaro.workflow.oferta.Modalidad;
import com.alvaro.workflow.oferta.TipoContrato;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OfertaRequest(
		@NotBlank(message = "El título es obligatorio")
		@Size(max = 150, message = "El título no puede superar los 150 caracteres")
		String titulo,

		@NotBlank(message = "La descripción es obligatoria")
		@Size(max = 10000, message = "La descripción no puede superar los 10000 caracteres")
		String descripcion,

		@NotBlank(message = "La ubicación es obligatoria")
		@Size(max = 150, message = "La ubicación no puede superar los 150 caracteres")
		String ubicacion,

		@NotNull(message = "La modalidad es obligatoria")
		Modalidad modalidad,

		@NotNull(message = "El tipo de contrato es obligatorio")
		TipoContrato tipoContrato,

		// Obligatorio: la Directiva (UE) 2023/970 exige indicar el salario en las ofertas
		@Schema(description = "Salario bruto anual mínimo en euros")
		@NotNull(message = "El salario mínimo es obligatorio")
		@Positive(message = "El salario mínimo debe ser mayor que cero")
		Integer salarioMinimo,

		@Schema(description = "Salario bruto anual máximo en euros")
		@NotNull(message = "El salario máximo es obligatorio")
		@Positive(message = "El salario máximo debe ser mayor que cero")
		Integer salarioMaximo) {
}
