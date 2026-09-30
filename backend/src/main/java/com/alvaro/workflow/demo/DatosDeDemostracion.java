package com.alvaro.workflow.demo;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.alvaro.workflow.auth.AuthService;
import com.alvaro.workflow.auth.dto.RegistroRequest;
import com.alvaro.workflow.candidatura.CandidaturaService;
import com.alvaro.workflow.candidatura.EstadoCandidatura;
import com.alvaro.workflow.candidatura.dto.CandidaturaRequest;
import com.alvaro.workflow.empresa.EmpresaService;
import com.alvaro.workflow.empresa.dto.EmpresaRequest;
import com.alvaro.workflow.oferta.Modalidad;
import com.alvaro.workflow.oferta.OfertaService;
import com.alvaro.workflow.oferta.TipoContrato;
import com.alvaro.workflow.oferta.dto.OfertaRequest;
import com.alvaro.workflow.usuario.Rol;
import com.alvaro.workflow.usuario.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carga empresas, ofertas y candidaturas de ejemplo al arrancar con el perfil "demo", para que quien
 * pruebe el proyecto vea el portal con contenido. Solo actúa si la base de datos está vacía.
 * <p>
 * Todo se crea a través de los servicios, con las mismas reglas que un usuario real. Después se
 * retrasan las fechas para que las ofertas y candidaturas tengan antigüedades distintas.
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DatosDeDemostracion {

	/** Contraseña de todas las cuentas de ejemplo (aparece en el README). */
	public static final String PASSWORD = "demo-workflow";

	private final UsuarioRepository usuarios;
	private final AuthService authService;
	private final EmpresaService empresaService;
	private final OfertaService ofertaService;
	private final CandidaturaService candidaturaService;
	private final JdbcTemplate jdbc;

	private record EmpresaDemo(String email, String nombre, String ubicacion, String web, String descripcion) {
	}

	private record OfertaDemo(String clave, String email, String titulo, String ubicacion, Modalidad modalidad,
			TipoContrato contrato, int salarioMinimo, int salarioMaximo, int hace, String descripcion) {
	}

	private record CandidatoDemo(String email, String nombre) {
	}

	/**
	 * @param hace días desde que se envió
	 * @param revision días desde que pasó a revisión, o null
	 * @param resolucion días desde que se resolvió, o null
	 */
	private record CandidaturaDemo(String candidato, String oferta, EstadoCandidatura estado, int hace,
			Integer revision, Integer resolucion, String carta) {
	}

	private static final List<EmpresaDemo> EMPRESAS = List.of(
			new EmpresaDemo("rrhh@lumen.test", "Lumen Seguros", "Madrid", "https://lumen.example",
					"Seguros para personas y pymes desde 1998.\nSomos 40 personas en el equipo de tecnología y "
							+ "desplegamos varias veces al día."),
			new EmpresaDemo("personas@brisa.test", "Brisa Logística", "Zaragoza", "https://brisa.example",
					"Operador logístico con tres almacenes en el valle del Ebro y 300 personas en plantilla."),
			new EmpresaDemo("hola@tallerocho.test", "Taller Ocho", "Valencia", "https://tallerocho.example",
					"Estudio de producto digital. Diseñamos y construimos aplicaciones para ONG y administraciones."),
			new EmpresaDemo("empleo@norte.test", "Norte Energía", "Bilbao", "https://norte.example",
					"Instalamos y mantenemos parques solares y eólicos en el norte peninsular."),
			new EmpresaDemo("equipo@almendro.test", "Clínica Almendro", "Sevilla", null,
					"Clínica de fisioterapia y rehabilitación con dos centros en Sevilla."),
			new EmpresaDemo("trabajo@marmenor.test", "Hoteles Mar Menor", "Murcia", "https://marmenor.example",
					"Tres hoteles familiares en primera línea de playa, abiertos todo el año."));

	private static final List<OfertaDemo> OFERTAS = List.of(
			new OfertaDemo("java", "rrhh@lumen.test", "Desarrollador/a Java Backend", "Madrid", Modalidad.REMOTO,
					TipoContrato.INDEFINIDO, 38000, 45000, 9,
					"Buscamos a alguien para el equipo que mantiene el motor de pólizas.\n\n"
							+ "Trabajarás con Java 25 y Spring Boot, con revisión de código entre compañeros y "
							+ "despliegues diarios.\n\nOfrecemos 23 días de vacaciones, horario flexible y "
							+ "presupuesto anual de formación."),
			new OfertaDemo("qa", "rrhh@lumen.test", "Ingeniero/a de QA", "Madrid", Modalidad.HIBRIDO,
					TipoContrato.INDEFINIDO, 32000, 38000, 4,
					"Automatizarás pruebas de la web de clientes con Playwright y definirás la estrategia de calidad "
							+ "junto al equipo de producto.\n\nDos días a la semana en la oficina de Chamberí."),
			new OfertaDemo("datos", "rrhh@lumen.test", "Analista de datos en prácticas", "Madrid",
					Modalidad.HIBRIDO, TipoContrato.PRACTICAS, 14000, 14000, 1,
					"Seis meses en el equipo de riesgos, preparando informes con SQL y Python.\n"
							+ "Tutora asignada y posibilidad de incorporación al terminar."),
			new OfertaDemo("almacen", "personas@brisa.test", "Responsable de almacén", "Zaragoza",
					Modalidad.PRESENCIAL, TipoContrato.INDEFINIDO, 28000, 32000, 12,
					"Coordinarás un equipo de 12 personas en el turno de mañana.\n\n"
							+ "Buscamos experiencia en gestión de stocks y ganas de mejorar procesos."),
			new OfertaDemo("carretilla", "personas@brisa.test", "Carretillero/a", "Zaragoza", Modalidad.PRESENCIAL,
					TipoContrato.TEMPORAL, 21000, 23000, 3,
					"Contrato de seis meses con opción a indefinido. Turno de tarde.\n"
							+ "Imprescindible carné de carretillero en vigor."),
			new OfertaDemo("trafico", "personas@brisa.test", "Técnico/a de tráfico", "Zaragoza", Modalidad.HIBRIDO,
					TipoContrato.INDEFINIDO, 26000, 30000, 20,
					"Planificarás las rutas de reparto de la flota propia y de los transportistas colaboradores."),
			new OfertaDemo("angular", "hola@tallerocho.test", "Desarrollador/a Frontend Angular", "Valencia",
					Modalidad.REMOTO, TipoContrato.INDEFINIDO, 34000, 42000, 6,
					"Construirás interfaces accesibles para administraciones públicas con Angular y signals.\n\n"
							+ "Nos importa la accesibilidad: todas nuestras aplicaciones cumplen WCAG 2.2 AA."),
			new OfertaDemo("ux", "hola@tallerocho.test", "Diseñador/a UX/UI", "Valencia", Modalidad.HIBRIDO,
					TipoContrato.INDEFINIDO, 30000, 36000, 15,
					"Investigarás con usuarios reales y diseñarás en Figma junto al equipo de desarrollo."),
			new OfertaDemo("pm", "hola@tallerocho.test", "Product manager freelance", "Valencia", Modalidad.REMOTO,
					TipoContrato.FREELANCE, 45000, 55000, 25,
					"Proyecto de un año para una fundación: priorizar, hablar con usuarios y coordinar al equipo."),
			new OfertaDemo("tecnico", "empleo@norte.test", "Técnico/a de mantenimiento eólico", "Bilbao",
					Modalidad.PRESENCIAL, TipoContrato.INDEFINIDO, 27000, 33000, 8,
					"Mantenimiento preventivo y correctivo de aerogeneradores.\n"
							+ "Formación en trabajos en altura a cargo de la empresa."),
			new OfertaDemo("ingeniera", "empleo@norte.test", "Ingeniero/a de proyectos solares", "Bilbao",
					Modalidad.HIBRIDO, TipoContrato.INDEFINIDO, 36000, 44000, 30,
					"Dirigirás la ejecución de instalaciones fotovoltaicas desde el diseño hasta la puesta en marcha."),
			new OfertaDemo("fisio", "equipo@almendro.test", "Fisioterapeuta", "Sevilla", Modalidad.PRESENCIAL,
					TipoContrato.INDEFINIDO, 25000, 29000, 5,
					"Jornada completa de lunes a viernes. Valoraremos formación en fisioterapia deportiva."),
			new OfertaDemo("recepcion", "equipo@almendro.test", "Recepcionista", "Sevilla", Modalidad.PRESENCIAL,
					TipoContrato.TEMPORAL, 18000, 19000, 2,
					"Sustitución de un año. Atención a pacientes, citas y facturación."),
			new OfertaDemo("cocina", "trabajo@marmenor.test", "Jefe/a de cocina", "Murcia", Modalidad.PRESENCIAL,
					TipoContrato.INDEFINIDO, 30000, 34000, 11,
					"Liderarás la cocina del hotel principal (180 cubiertos) y la carta de temporada."),
			new OfertaDemo("camarero", "trabajo@marmenor.test", "Camarero/a de sala", "Murcia", Modalidad.PRESENCIAL,
					TipoContrato.TEMPORAL, 19000, 21000, 0,
					"Temporada de invierno con alojamiento incluido para quien venga de fuera."));

	private static final List<CandidatoDemo> CANDIDATOS = List.of(
			new CandidatoDemo("ana@demo.test", "Ana García"),
			new CandidatoDemo("luis@demo.test", "Luis Ortega"),
			new CandidatoDemo("marta@demo.test", "Marta Sanz"));

	private static final List<CandidaturaDemo> CANDIDATURAS = List.of(
			new CandidaturaDemo("ana@demo.test", "java", EstadoCandidatura.EN_REVISION, 8, 2, null,
					"Llevo cuatro años con Spring Boot y me encantaría trabajar en un producto propio."),
			new CandidaturaDemo("ana@demo.test", "angular", EstadoCandidatura.ACEPTADA, 6, 4, 1, null),
			new CandidaturaDemo("ana@demo.test", "ingeniera", EstadoCandidatura.RECHAZADA, 20, null, 14, null),
			new CandidaturaDemo("ana@demo.test", "qa", EstadoCandidatura.PENDIENTE, 1, null, null, null),
			new CandidaturaDemo("luis@demo.test", "java", EstadoCandidatura.PENDIENTE, 7, null, null,
					"Vengo del mundo .NET y tengo muchas ganas de pasarme a Java."),
			new CandidaturaDemo("luis@demo.test", "almacen", EstadoCandidatura.RETIRADA, 10, null, 5, null),
			new CandidaturaDemo("marta@demo.test", "java", EstadoCandidatura.PENDIENTE, 2, null, null, null),
			new CandidaturaDemo("marta@demo.test", "ux", EstadoCandidatura.EN_REVISION, 12, 6, null,
					"Os envío mi portfolio: marta-sanz.example"));

	@EventListener(ApplicationReadyEvent.class)
	public void cargar() {
		if (usuarios.count() > 0) {
			log.info("La base de datos ya tiene usuarios: no se cargan los datos de demostración");
			return;
		}

		Map<String, UUID> idPorEmail = new HashMap<>();
		for (EmpresaDemo empresa : EMPRESAS) {
			UUID id = registrar(empresa.email(), empresa.nombre(), Rol.EMPRESA);
			empresaService.actualizarDelUsuario(id, new EmpresaRequest(empresa.nombre(), empresa.descripcion(),
					empresa.web(), empresa.ubicacion()));
			idPorEmail.put(empresa.email(), id);
		}
		for (CandidatoDemo candidato : CANDIDATOS) {
			idPorEmail.put(candidato.email(), registrar(candidato.email(), candidato.nombre(), Rol.CANDIDATO));
		}

		Map<String, UUID> ofertaPorClave = new HashMap<>();
		Map<String, UUID> empresaDeOferta = new HashMap<>();
		for (OfertaDemo oferta : OFERTAS) {
			UUID empresaId = idPorEmail.get(oferta.email());
			UUID ofertaId = ofertaService.publicar(empresaId, new OfertaRequest(oferta.titulo(), oferta.descripcion(),
					oferta.ubicacion(), oferta.modalidad(), oferta.contrato(), oferta.salarioMinimo(),
					oferta.salarioMaximo())).id();
			ofertaPorClave.put(oferta.clave(), ofertaId);
			empresaDeOferta.put(oferta.clave(), empresaId);
			jdbc.update("update ofertas set fecha_creacion = ?, fecha_actualizacion = ? where id = ?",
					hace(oferta.hace()), hace(oferta.hace()), ofertaId);
		}

		for (CandidaturaDemo candidatura : CANDIDATURAS) {
			UUID candidatoId = idPorEmail.get(candidatura.candidato());
			UUID ofertaId = ofertaPorClave.get(candidatura.oferta());
			UUID empresaId = empresaDeOferta.get(candidatura.oferta());
			UUID id = candidaturaService
					.inscribirse(candidatoId, ofertaId, new CandidaturaRequest(candidatura.carta())).id();
			avanzar(id, candidatura.estado(), candidatoId, empresaId, candidatura.revision() != null);
			jdbc.update("""
					update candidaturas
					set fecha_creacion = ?, fecha_revision = ?, fecha_resolucion = ?, fecha_actualizacion = ?
					where id = ?""",
					hace(candidatura.hace()), hace(candidatura.revision()), hace(candidatura.resolucion()),
					hace(ultimoCambio(candidatura)), id);
		}

		log.info("Cargados los datos de demostración: {} empresas, {} ofertas, {} candidatos y {} candidaturas",
				EMPRESAS.size(), OFERTAS.size(), CANDIDATOS.size(), CANDIDATURAS.size());
	}

	private UUID registrar(String email, String nombre, Rol rol) {
		return authService.crearCuentaVerificada(new RegistroRequest(email, PASSWORD, nombre, rol)).getId();
	}

	/** Lleva la candidatura a su estado de ejemplo por el mismo camino que seguiría en la aplicación. */
	private void avanzar(UUID candidaturaId, EstadoCandidatura estado, UUID candidatoId, UUID empresaId,
			boolean pasoPorRevision) {
		if (pasoPorRevision) {
			candidaturaService.cambiarEstado(empresaId, candidaturaId, EstadoCandidatura.EN_REVISION);
		}
		switch (estado) {
			case ACEPTADA, RECHAZADA -> candidaturaService.cambiarEstado(empresaId, candidaturaId, estado);
			case RETIRADA -> candidaturaService.retirar(candidatoId, candidaturaId);
			case PENDIENTE, EN_REVISION -> {
				// Ya está en su estado
			}
		}
	}

	private static int ultimoCambio(CandidaturaDemo candidatura) {
		if (candidatura.resolucion() != null) {
			return candidatura.resolucion();
		}
		return candidatura.revision() != null ? candidatura.revision() : candidatura.hace();
	}

	private static Timestamp hace(Integer dias) {
		return dias == null ? null : Timestamp.from(Instant.now().minus(Duration.ofDays(dias)));
	}

}
