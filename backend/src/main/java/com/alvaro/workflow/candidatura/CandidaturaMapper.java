package com.alvaro.workflow.candidatura;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.alvaro.workflow.candidatura.dto.CandidaturaRecibidaResponse;
import com.alvaro.workflow.candidatura.dto.MiCandidaturaResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CandidaturaMapper {

	MiCandidaturaResponse toMiCandidatura(Candidatura candidatura);

	CandidaturaRecibidaResponse toRecibida(Candidatura candidatura);

}
