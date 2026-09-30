package com.alvaro.workflow.moderacion;

import org.springframework.stereotype.Component;

import com.alvaro.workflow.correo.ColaDeCorreo;
import com.alvaro.workflow.correo.CorreoProperties;
import com.alvaro.workflow.correo.Mensaje;
import com.alvaro.workflow.empresa.Empresa;
import com.alvaro.workflow.oferta.Oferta;

import lombok.RequiredArgsConstructor;

/** Correos de moderación: al equipo cuando llega una denuncia y a la empresa cuando se actúa sobre ella. */
@Component
@RequiredArgsConstructor
class AvisosDeModeracion {

	private final ColaDeCorreo colaDeCorreo;
	private final CorreoProperties correo;

	void denunciaRecibida(Denuncia denuncia) {
		Oferta oferta = denuncia.getOferta();
		colaDeCorreo.encolar(new Mensaje(correo.contacto(), "Denuncia de la oferta " + oferta.getTitulo(), """
				Han denunciado la oferta "%s", de %s.

				Motivo: %s
				%s
				Revísala en el panel de moderación:

				%s
				""".formatted(oferta.getTitulo(), oferta.getEmpresa().getNombre(), denuncia.getMotivo(),
				denuncia.getDetalle() == null ? "" : "Explicación: " + denuncia.getDetalle() + "\n",
				correo.enlace("/admin"))));
	}

	void ofertaRetirada(Oferta oferta) {
		colaDeCorreo.encolar(new Mensaje(oferta.getEmpresa().getUsuario().getEmail(),
				"Hemos retirado tu oferta " + oferta.getTitulo(), """
						Hemos revisado tu oferta "%s" y la hemos retirado porque incumple las normas de publicación.

						Ya no aparece en el buscador ni admite candidaturas.

						Si crees que es un error, escribe a %s y la volveremos a revisar.
						""".formatted(oferta.getTitulo(), correo.contacto())));
	}

	void cuentaSuspendida(Empresa empresa) {
		colaDeCorreo.encolar(new Mensaje(empresa.getUsuario().getEmail(), "Hemos suspendido tu cuenta de Workflow", """
				Hemos suspendido la cuenta de %s por incumplir las normas de uso. Sus ofertas ya no son visibles \
				y no se puede iniciar sesión con ella.

				Si crees que es un error, escribe a %s y lo volveremos a revisar.
				""".formatted(empresa.getNombre(), correo.contacto())));
	}

}
