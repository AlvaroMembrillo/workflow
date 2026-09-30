package com.alvaro.workflow.cv;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CurriculumRepository extends JpaRepository<Curriculum, UUID> {

	@Query("""
			select new com.alvaro.workflow.cv.CurriculumResumen(c.usuarioId, c.nombreFichero, c.tamano, c.fechaSubida)
			from Curriculum c where c.usuarioId = :usuarioId
			""")
	Optional<CurriculumResumen> resumenDe(UUID usuarioId);

	@Query("""
			select new com.alvaro.workflow.cv.CurriculumResumen(c.usuarioId, c.nombreFichero, c.tamano, c.fechaSubida)
			from Curriculum c where c.usuarioId in :usuarioIds
			""")
	List<CurriculumResumen> resumenesDe(Collection<UUID> usuarioIds);

}
