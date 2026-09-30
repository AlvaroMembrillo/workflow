package com.alvaro.workflow.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.alvaro.workflow.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import com.alvaro.workflow.correo.BuzonDePrueba;
import com.alvaro.workflow.correo.CorreoDePruebaConfiguration;

/** Arranca la aplicación con el perfil "demo" y comprueba que los datos de ejemplo se cargan bien. */
@SpringBootTest(properties = "app.jwt.clave-privada=target/jwt-demo/clave-privada.pem")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import({ TestcontainersConfiguration.class, CorreoDePruebaConfiguration.class })
class DatosDeDemostracionTests {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private DatosDeDemostracion datos;

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private BuzonDePrueba buzon;

	@Test
	void cargaEmpresasOfertasYCandidaturasEnTodosLosEstados() {
		assertThat(contar("select count(*) from empresas")).isEqualTo(6);
		assertThat(contar("select count(*) from ofertas where estado = 'ABIERTA'")).isEqualTo(15);
		assertThat(contar("select count(*) from usuarios where rol = 'CANDIDATO'")).isEqualTo(3);
		assertThat(contar("select count(*) from curriculos")).isEqualTo(3);

		Map<String, Long> porEstado = new HashMap<>();
		jdbc.query("select estado, count(*) from candidaturas group by estado",
				fila -> { porEstado.put(fila.getString(1), fila.getLong(2)); });
		assertThat(porEstado).containsOnlyKeys("PENDIENTE", "EN_REVISION", "ACEPTADA", "RECHAZADA", "RETIRADA");
	}

	@Test
	void lasFechasSonCoherentesConElEstado() {
		assertThat(contar("select count(*) from candidaturas where estado in ('ACEPTADA', 'RECHAZADA', 'RETIRADA') "
				+ "and fecha_resolucion is null")).isZero();
		assertThat(contar("select count(*) from candidaturas where estado = 'EN_REVISION' and fecha_revision is null"))
				.isZero();
		assertThat(contar("select count(*) from candidaturas where fecha_revision < fecha_creacion "
				+ "or fecha_resolucion < fecha_creacion")).isZero();
	}

	@Test
	void seEntraConLasCuentasDelReadme() {
		assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "ana@demo.test", "password": "%s"}
						""".formatted(DatosDeDemostracion.PASSWORD))
				.exchange()).hasStatusOk();
		assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "rrhh@lumen.test", "password": "%s"}
						""".formatted(DatosDeDemostracion.PASSWORD))
				.exchange()).hasStatusOk();
	}

	@Test
	void lasCuentasDeEjemploTienenElEmailVerificadoYRecibenLosAvisos() {
		assertThat(contar("select count(*) from usuarios where email_verificado_en is null")).isZero();
		// Sin correos de verificación; sí los avisos de las candidaturas de ejemplo
		assertThat(buzon.para("rrhh@lumen.test")).isNotEmpty()
				.allSatisfy(mensaje -> assertThat(mensaje.asunto()).startsWith("Nueva candidatura para "));
		assertThat(buzon.para("ana@demo.test")).extracting("asunto").contains(
				"Tu candidatura a Desarrollador/a Java Backend está en revisión",
				"Tu candidatura a Desarrollador/a Frontend Angular ha sido seleccionada");
	}

	@Test
	void creaLaCuentaDeModeracionYUnaDenunciaPendienteParaRevisar() throws Exception {
		assertThat(contar("select count(*) from usuarios where rol = 'ADMIN' and email = 'admin@demo.test'")).isEqualTo(1);
		assertThat(contar("select count(*) from denuncias where estado = 'PENDIENTE'")).isEqualTo(1);

		String token = JsonPath.read(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "admin@demo.test", "password": "%s"}
						""".formatted(DatosDeDemostracion.PASSWORD))
				.exchange().getResponse().getContentAsString(), "$.accessToken");
		assertThat(mvc.get().uri("/api/admin/denuncias").header("Authorization", "Bearer " + token).exchange())
				.bodyJson().extractingPath("$.content[0].oferta.titulo").isEqualTo("Product manager freelance");
	}

	@Test
	void noDuplicaNadaSiLaBaseDeDatosYaTieneDatos() {
		long ofertas = contar("select count(*) from ofertas");

		datos.cargar();

		assertThat(contar("select count(*) from ofertas")).isEqualTo(ofertas);
	}

	private long contar(String sql) {
		return jdbc.queryForObject(sql, Long.class);
	}

}
