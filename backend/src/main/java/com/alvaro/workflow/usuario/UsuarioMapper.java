package com.alvaro.workflow.usuario;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UsuarioMapper {

	UsuarioResponse toResponse(Usuario usuario);

}
