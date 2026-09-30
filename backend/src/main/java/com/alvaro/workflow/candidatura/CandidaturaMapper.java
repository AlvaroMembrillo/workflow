package com.alvaro.workflow.candidatura;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.alvaro.workflow.candidatura.dto.CandidaturaRecibidaResponse;
import com.alvaro.workflow.candidatura.dto.MiCandidaturaResponse;
import com.alvaro.workflow.cv.CurriculumResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CandidaturaMapper {

	MiCandidaturaResponse toMiCandidatura(Candidatura candidatura);

	/** @param cv el currículum del candidato, o null si no ha subido ninguno */
	@Mapping(target = "cv", source = "cv")
	CandidaturaRecibidaResponse toRecibida(Candidatura candidatura, CurriculumResponse cv);

}
