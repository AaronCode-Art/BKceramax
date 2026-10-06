package com.ceramax.api.mappers.catalogo;

import com.ceramax.api.dto.request.GaleriaImagenRequest;
import com.ceramax.api.dto.response.GaleriaImagenResponse;
import com.ceramax.api.model.catalogo.GaleriaImagen;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GaleriaImagenMapper {
    @Mapping(target = "producto", ignore = true)
    GaleriaImagen toEntity(GaleriaImagenRequest request);

    @Mapping(target = "productoId", source = "producto.id")
    GaleriaImagenResponse toResponse(GaleriaImagen entity);

    @Mapping(target = "producto", ignore = true)
    void updateEntityFromRequest(GaleriaImagenRequest request, @MappingTarget GaleriaImagen entity);
}
