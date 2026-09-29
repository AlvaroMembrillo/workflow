package com.alvaro.workflow.candidatura.dto;

import jakarta.validation.constraints.Size;

public record CandidaturaRequest(
		@Size(max = 5000, message = "La carta de presentación no puede superar los 5000 caracteres")
		String cartaPresentacion) {
}
