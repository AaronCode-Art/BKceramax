package com.ceramax.api.mappers.catalogo;

import com.ceramax.api.dto.request.ProductoRequest;
import com.ceramax.api.dto.response.ProductoResponse;
import com.ceramax.api.model.catalogo.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductoMapper {
    @Mapping(target = "categoria.id", source = "categoriaId")
    Producto toEntity(ProductoRequest request);

    @Mapping(target = "categoriaId", source = "categoria.id")
    @Mapping(target = "categoriaNombre", source = "categoria.nombre")
    ProductoResponse toResponse(Producto product);

    @Mapping(target = "categoria.id", source = "categoriaId")
    void updateEntityFromRequest(ProductoRequest request, @MappingTarget Producto entity);
}
