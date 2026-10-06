package com.ceramax.api.mappers.acceso;

import com.ceramax.api.dto.request.SucursalRequest;
import com.ceramax.api.dto.response.SucursalResponse;
import com.ceramax.api.model.acceso.Sucursal;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SucursalMapper {
    Sucursal toEntity(SucursalRequest request);
    SucursalResponse toResponse(Sucursal entity);
    void updateEntityFromRequest(SucursalRequest request, @MappingTarget Sucursal entity);
}
