package com.ceramax.api.mappers.acceso;

import com.ceramax.api.dto.request.RolRequest;
import com.ceramax.api.dto.response.RolResponse;
import com.ceramax.api.model.acceso.Rol;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RolMapper {
    Rol toEntity(RolRequest request);
    RolResponse toResponse(Rol entity);
    void updateEntityFromRequest(RolRequest request, @MappingTarget Rol entity);
}
