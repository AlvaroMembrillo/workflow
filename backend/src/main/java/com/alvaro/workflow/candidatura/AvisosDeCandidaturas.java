package com.alvaro.workflow.candidatura;

import org.springframework.stereotype.Component;

import com.alvaro.workflow.correo.ColaDeCorreo;
import com.alvaro.workflow.correo.CorreoProperties;
import com.alvaro.workflow.correo.Mensaje;
import com.alvaro.workflow.oferta.Oferta;
import com.alvaro.workflow.usuario.Usuario;

import lombok.RequiredArgsConstructor;

/**
 * Avisa por correo de lo que pasa con una candidatura: a la empresa cuando recibe una y al candidato
 * cuando la empresa la mueve, para que no tenga que entrar a mirar si hay novedades.
 * Solo escribe a quien tiene el email verificado y no ha desactivado los avisos.
 */
@Component
@RequiredArgsConstructor
class AvisosDeCandidaturas {

	private final ColaDeCorreo colaDeCorreo;
	private final CorreoProperties correo;

	void recibida(Candidatura candidatura) {
		Oferta oferta = candidatura.getOferta();
		Usuario empresa = oferta.getEmpresa().getUsuario();
		if (!empresa.recibeAvisos()) {
			return;
		}
		colaDeCorreo.encolar(new Mensaje(empresa.getEmail(), "Nueva candidatura para " + oferta.getTitulo(), """
				%s se ha inscrito en tu oferta "%s".

				Puedes ver su candidatura y responderle aquí:

				%s

				Los candidatos ven en qué punto está su candidatura y desde cuándo espera respuesta.
				%s""".formatted(candidatura.getCandidato().getNombre(), oferta.getTitulo(),
				correo.enlace("/empresa/ofertas/" + oferta.getId() + "/candidaturas"), pie())));
	}

	void estadoCambiado(Candidatura candidatura) {
		Usuario candidato = candidatura.getCandidato();
		if (!candidato.recibeAvisos()) {
			return;
		}
		String titulo = candidatura.getOferta().getTitulo();
		String empresa = candidatura.getOferta().getEmpresa().getNombre();
		String asunto;
		String novedad;
		switch (candidatura.getEstado()) {
			case EN_REVISION -> {
				asunto = "Tu candidatura a " + titulo + " está en revisión";
				novedad = "%s está revisando tu candidatura a \"%s\".".formatted(empresa, titulo);
			}
			case ACEPTADA -> {
				asunto = "Tu candidatura a " + titulo + " ha sido seleccionada";
				novedad = "%s ha seleccionado tu candidatura a \"%s\". Se pondrá en contacto contigo en este email."
						.formatted(empresa, titulo);
			}
			case RECHAZADA -> {
				asunto = "Novedades de tu candidatura a " + titulo;
				novedad = "%s ha decidido no continuar con tu candidatura a \"%s\". Gracias por el tiempo que le has dedicado."
						.formatted(empresa, titulo);
			}
			default -> {
				// PENDIENTE y RETIRADA no las decide la empresa: no hay nada que avisar
				return;
			}
		}
		colaDeCorreo.encolar(new Mensaje(candidato.getEmail(), asunto, """
				Hola, %s:

				%s

				Puedes ver todas tus candidaturas y la fecha de cada paso aquí:

				%s
				%s""".formatted(candidato.getNombre(), novedad, correo.enlace("/mis-candidaturas"), pie())));
	}

	private String pie() {
		return "\nPuedes desactivar estos avisos en " + correo.enlace("/cuenta") + "\n";
	}

}
