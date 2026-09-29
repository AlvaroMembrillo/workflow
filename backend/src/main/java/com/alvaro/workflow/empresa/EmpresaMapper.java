package com.alvaro.workflow.empresa;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.alvaro.workflow.empresa.dto.EmpresaResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmpresaMapper {

	EmpresaResponse toResponse(Empresa empresa);

}
