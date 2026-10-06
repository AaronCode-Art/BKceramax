package com.ceramax.api.mappers.catalogo;

import com.ceramax.api.dto.request.CategoriaRequest;
import com.ceramax.api.dto.response.CategoriaResponse;
import com.ceramax.api.model.catalogo.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoriaMapper {
    Categoria toEntity(CategoriaRequest request);
    CategoriaResponse toResponse(Categoria entity);
    void updateEntityFromRequest(CategoriaRequest request, @MappingTarget Categoria entity);
}
