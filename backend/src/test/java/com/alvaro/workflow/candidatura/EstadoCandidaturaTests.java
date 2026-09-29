package com.alvaro.workflow.candidatura;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class EstadoCandidaturaTests {

	@ParameterizedTest(name = "{0} → {1}: {2}")
	@CsvSource({
			"PENDIENTE,   PENDIENTE,   false",
			"PENDIENTE,   EN_REVISION, true",
			"PENDIENTE,   ACEPTADA,    true",
			"PENDIENTE,   RECHAZADA,   true",
			"PENDIENTE,   RETIRADA,    true",
			"EN_REVISION, PENDIENTE,   false",
			"EN_REVISION, EN_REVISION, false",
			"EN_REVISION, ACEPTADA,    true",
			"EN_REVISION, RECHAZADA,   true",
			"EN_REVISION, RETIRADA,    true",
			"ACEPTADA,    PENDIENTE,   false",
			"ACEPTADA,    EN_REVISION, false",
			"ACEPTADA,    ACEPTADA,    false",
			"ACEPTADA,    RECHAZADA,   false",
			"ACEPTADA,    RETIRADA,    false",
			"RECHAZADA,   PENDIENTE,   false",
			"RECHAZADA,   EN_REVISION, false",
			"RECHAZADA,   ACEPTADA,    false",
			"RECHAZADA,   RECHAZADA,   false",
			"RECHAZADA,   RETIRADA,    false",
			"RETIRADA,    PENDIENTE,   false",
			"RETIRADA,    EN_REVISION, false",
			"RETIRADA,    ACEPTADA,    false",
			"RETIRADA,    RECHAZADA,   false",
			"RETIRADA,    RETIRADA,    false" })
	void soloPermiteAvanzarHastaUnEstadoFinal(EstadoCandidatura actual, EstadoCandidatura nuevo, boolean permitido) {
		assertThat(actual.puedeCambiarA(nuevo)).isEqualTo(permitido);
	}

	@ParameterizedTest
	@EnumSource(names = { "ACEPTADA", "RECHAZADA", "RETIRADA" })
	void losEstadosFinalesNoPermitenNingunCambio(EstadoCandidatura estado) {
		assertThat(estado.esFinal()).isTrue();
		for (EstadoCandidatura otro : EstadoCandidatura.values()) {
			assertThat(estado.puedeCambiarA(otro)).isFalse();
		}
	}

}
