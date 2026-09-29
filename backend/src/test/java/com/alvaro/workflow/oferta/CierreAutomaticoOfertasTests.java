package com.alvaro.workflow.oferta;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.alvaro.workflow.IntegrationTestBase;

class CierreAutomaticoOfertasTests extends IntegrationTestBase {

	@Autowired
	private CierreAutomaticoOfertas cierreAutomatico;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void cierraLasOfertasAbiertasSinActividadYDejaLasDemas() {
		String empresa = nuevaEmpresa();
		String antigua = publicarOferta(empresa);
		String reciente = publicarOferta(empresa);
		String antiguaYaCerrada = publicarOferta(empresa);
		patch("/api/ofertas/" + antiguaYaCerrada + "/estado", empresa, """
				{"estado": "CERRADA"}
				""");
		envejecer(antigua, Duration.ofDays(61));
		envejecer(antiguaYaCerrada, Duration.ofDays(61));
		envejecer(reciente, Duration.ofDays(59));

		int cerradas = cierreAutomatico.cerrarOfertasSinActividad();

		assertThat(cerradas).isGreaterThanOrEqualTo(1);
		assertThat(estado(antigua)).isEqualTo("CERRADA");
		assertThat(estado(reciente)).isEqualTo("ABIERTA");
		assertThat(estado(antiguaYaCerrada)).isEqualTo("CERRADA");
	}

	private String publicarOferta(String tokenEmpresa) {
		return leer(post("/api/ofertas", tokenEmpresa, """
				{"titulo": "Desarrollador Java", "descripcion": "Descripción", "ubicacion": "Madrid",
				 "modalidad": "REMOTO", "tipoContrato": "INDEFINIDO", "salarioMinimo": 30000, "salarioMaximo": 40000}
				"""), "$.id");
	}

	/** Simula que la oferta lleva ese tiempo sin cambios. */
	private void envejecer(String ofertaId, Duration tiempo) {
		jdbc.update("update ofertas set fecha_actualizacion = ? where id = ?",
				Timestamp.from(Instant.now().minus(tiempo)), UUID.fromString(ofertaId));
	}

	private String estado(String ofertaId) {
		return leer(get("/api/ofertas/" + ofertaId, null), "$.estado");
	}

}
