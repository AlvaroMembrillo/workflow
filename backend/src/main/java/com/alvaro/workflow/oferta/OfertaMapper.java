package com.alvaro.workflow.oferta;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.alvaro.workflow.oferta.dto.OfertaRequest;
import com.alvaro.workflow.oferta.dto.OfertaResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OfertaMapper {

	OfertaResponse toResponse(Oferta oferta);

	Oferta.DatosOferta toDatos(OfertaRequest request);

}
