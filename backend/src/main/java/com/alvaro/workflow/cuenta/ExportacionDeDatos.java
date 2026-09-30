package com.alvaro.workflow.cuenta;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.workflow.candidatura.CandidaturaRepository;
import com.alvaro.workflow.cuenta.MisDatos.CandidaturaEnviada;
import com.alvaro.workflow.cuenta.MisDatos.Cuenta;
import com.alvaro.workflow.cuenta.MisDatos.Curriculum;
import com.alvaro.workflow.cuenta.MisDatos.OfertaPublicada;
import com.alvaro.workflow.cuenta.MisDatos.PerfilDeEmpresa;
import com.alvaro.workflow.cv.CurriculumService;
import com.alvaro.workflow.empresa.Empresa;
import com.alvaro.workflow.empresa.EmpresaRepository;
import com.alvaro.workflow.moderacion.DenunciaService;
import com.alvaro.workflow.oferta.OfertaRepository;
import com.alvaro.workflow.usuario.Usuario;
import com.alvaro.workflow.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExportacionDeDatos {

	private final UsuarioService usuarioService;
	private final EmpresaRepository empresas;
	private final OfertaRepository ofertas;
	private final CandidaturaRepository candidaturas;
	private final CurriculumService curriculumService;
	private final DenunciaService denunciaService;

	@Transactional(readOnly = true)
	public MisDatos de(UUID usuarioId) {
		Usuario usuario = usuarioService.buscarPorId(usuarioId);
		Optional<Empresa> empresa = empresas.findByUsuarioId(usuarioId);

		List<OfertaPublicada> ofertasPublicadas = empresa
				.map(perfil -> ofertas.findByEmpresaIdOrderByFechaCreacionDesc(perfil.getId()).stream()
						.map(oferta -> new OfertaPublicada(oferta.getTitulo(), oferta.getDescripcion(),
								oferta.getUbicacion(), oferta.getModalidad(), oferta.getTipoContrato(),
								oferta.getSalarioMinimo(), oferta.getSalarioMaximo(), oferta.getEstado(),
								oferta.getFechaCreacion()))
						.toList())
				.orElse(List.of());

		List<CandidaturaEnviada> enviadas = candidaturas.findByCandidatoIdOrderByFechaCreacionDesc(usuarioId).stream()
				.map(candidatura -> new CandidaturaEnviada(candidatura.getOferta().getTitulo(),
						candidatura.getOferta().getEmpresa().getNombre(), candidatura.getEstado(),
						candidatura.getCartaPresentacion(), candidatura.getFechaCreacion(),
						candidatura.getFechaRevision(), candidatura.getFechaResolucion()))
				.toList();

		return new MisDatos(
				Instant.now(),
				new Cuenta(usuario.getId(), usuario.getEmail(), usuario.getNombre(), usuario.getRol(),
						usuario.getFechaCreacion(), usuario.getEmailVerificadoEn(), usuario.getCondicionesAceptadasEn(),
						usuario.isAvisosPorCorreo()),
				empresa.map(perfil -> new PerfilDeEmpresa(perfil.getNombre(), perfil.getDescripcion(),
						perfil.getSitioWeb(), perfil.getUbicacion(), perfil.getFechaCreacion())).orElse(null),
				ofertasPublicadas,
				curriculumService.resumenSiExiste(usuarioId)
						.map(cv -> new Curriculum(cv.nombreFichero(), cv.tamano(), cv.fechaSubida())).orElse(null),
				enviadas,
				denunciaService.hechasPor(usuarioId));
	}

}
