package com.alvaro.workflow.candidatura;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EstadoCandidaturaTests {

	@ParameterizedTest(name = "{0} → {1}: {2}")
	@CsvSource({
			"PENDIENTE,   PENDIENTE,   false",
			"PENDIENTE,   EN_REVISION, true",
			"PENDIENTE,   ACEPTADA,    true",
			"PENDIENTE,   RECHAZADA,   true",
			"EN_REVISION, PENDIENTE,   false",
			"EN_REVISION, EN_REVISION, false",
			"EN_REVISION, ACEPTADA,    true",
			"EN_REVISION, RECHAZADA,   true",
			"ACEPTADA,    PENDIENTE,   false",
			"ACEPTADA,    EN_REVISION, false",
			"ACEPTADA,    ACEPTADA,    false",
			"ACEPTADA,    RECHAZADA,   false",
			"RECHAZADA,   PENDIENTE,   false",
			"RECHAZADA,   EN_REVISION, false",
			"RECHAZADA,   ACEPTADA,    false",
			"RECHAZADA,   RECHAZADA,   false" })
	void soloPermiteAvanzarHastaUnaDecisionFinal(EstadoCandidatura actual, EstadoCandidatura nuevo, boolean permitido) {
		assertThat(actual.puedeCambiarA(nuevo)).isEqualTo(permitido);
	}

}
