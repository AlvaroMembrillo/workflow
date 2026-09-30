package com.alvaro.workflow.cuenta;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.candidatura.Candidatura;
import com.alvaro.workflow.candidatura.CandidaturaRepository;
import com.alvaro.workflow.candidatura.EstadoCandidatura;
import com.alvaro.workflow.common.CampoNoValidoException;
import com.alvaro.workflow.common.PeticionNoValidaException;
import com.alvaro.workflow.correo.ColaDeCorreo;
import com.alvaro.workflow.correo.CorreoProperties;
import com.alvaro.workflow.correo.Mensaje;
import com.alvaro.workflow.empresa.Empresa;
import com.alvaro.workflow.empresa.EmpresaRepository;
import com.alvaro.workflow.oferta.OfertaRepository;
import com.alvaro.workflow.usuario.Rol;
import com.alvaro.workflow.usuario.Usuario;
import com.alvaro.workflow.usuario.UsuarioRepository;
import com.alvaro.workflow.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Borra cuentas con todos sus datos (derecho de supresión, RGPD artículo 17). No se conserva nada: ni
 * copia desactivada ni datos anonimizados, salvo las denuncias que hizo, que se quedan sin autor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorradoDeCuentas {

	private final UsuarioService usuarioService;
	private final UsuarioRepository usuarios;
	private final EmpresaRepository empresas;
	private final OfertaRepository ofertas;
	private final CandidaturaRepository candidaturas;
	private final PasswordEncoder passwordEncoder;
	private final ColaDeCorreo colaDeCorreo;
	private final CorreoProperties correo;
	private final CuentasProperties properties;

	/** El usuario borra su propia cuenta. Tiene que confirmar con su contraseña. */
	@Transactional
	public void borrarLaPropia(UUID usuarioId, String password) {
		Usuario usuario = usuarioService.buscarPorId(usuarioId);
		if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
			throw new CampoNoValidoException("password", "La contraseña no es correcta");
		}
		if (usuario.getRol() == Rol.ADMIN) {
			throw new PeticionNoValidaException("Cuenta de administración",
					"Las cuentas de administración no se borran desde la web");
		}
		String email = usuario.getEmail();
		String nombre = usuario.getNombre();
		borrar(usuario);
		colaDeCorreo.encolar(new Mensaje(email, "Hemos borrado tu cuenta de Workflow", """
				Hola, %s:

				Hemos borrado tu cuenta y todos los datos asociados a ella, como pediste.

				Si no has sido tú, escribe cuanto antes a %s.
				""".formatted(nombre, correo.contacto())));
	}

	/** Cada noche se borran las cuentas que llevan demasiado tiempo sin confirmar su email. */
	@Scheduled(cron = "0 15 4 * * *", zone = "Europe/Madrid")
	@Transactional
	public int borrarLasQueNoSeVerificaron() {
		List<UUID> ids = usuarios.idsSinVerificarCreadasAntesDe(Instant.now().minus(properties.borrarSinVerificarTras()));
		ids.forEach(id -> borrar(usuarioService.buscarPorId(id)));
		if (!ids.isEmpty()) {
			log.info("Borradas {} cuentas que no confirmaron su email en {} días", ids.size(),
					properties.borrarSinVerificarTras().toDays());
		}
		return ids.size();
	}

	private void borrar(Usuario usuario) {
		UUID usuarioId = usuario.getId();
		empresas.findByUsuarioId(usuarioId).ifPresent(this::borrarEmpresa);
		candidaturas.borrarDelCandidato(usuarioId);
		// El currículum, los enlaces pendientes y las sesiones se borran en cascada con el usuario
		usuarios.borrarPorId(usuarioId);
	}

	private void borrarEmpresa(Empresa empresa) {
		// Quien esperaba respuesta de esta empresa se entera de que su candidatura ya no existe
		List<Candidatura> enEspera = candidaturas.findByOfertaEmpresaIdAndEstadoIn(empresa.getId(),
				List.of(EstadoCandidatura.PENDIENTE, EstadoCandidatura.EN_REVISION));
		for (Candidatura candidatura : enEspera) {
			Usuario candidato = candidatura.getCandidato();
			if (candidato.recibeAvisos()) {
				colaDeCorreo.encolar(new Mensaje(candidato.getEmail(),
						"Tu candidatura a " + candidatura.getOferta().getTitulo() + " ya no está disponible", """
								Hola, %s:

								%s ha cerrado su cuenta en Workflow, así que su oferta "%s" y tu candidatura han \
								dejado de existir.

								Puedes ver el resto de tus candidaturas aquí:

								%s
								""".formatted(candidato.getNombre(), empresa.getNombre(),
								candidatura.getOferta().getTitulo(), correo.enlace("/mis-candidaturas"))));
			}
		}
		candidaturas.borrarDeLasOfertasDeLaEmpresa(empresa.getId());
		ofertas.borrarDeLaEmpresa(empresa.getId());
		empresas.borrarPorId(empresa.getId());
	}

}
