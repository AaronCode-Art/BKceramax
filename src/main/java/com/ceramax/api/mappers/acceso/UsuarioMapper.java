package com.ceramax.api.mappers.acceso;

import com.ceramax.api.dto.request.UsuarioRequest;
import com.ceramax.api.dto.response.UsuarioResponse;
import com.ceramax.api.model.acceso.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UsuarioMapper {
    @Mapping(target = "rol.id", source = "rolId")
    @Mapping(target = "sucursal.id", source = "sucursalId")
    @Mapping(target = "passwordHash", ignore = true)
    Usuario toEntity(UsuarioRequest request);

    @Mapping(target = "rolId", source = "rol.id")
    @Mapping(target = "rolCodigo", source = "rol.codigo")
    @Mapping(target = "sucursalId", source = "sucursal.id")
    UsuarioResponse toResponse(Usuario entity);

    @Mapping(target = "rol.id", source = "rolId")
    @Mapping(target = "sucursal.id", source = "sucursalId")
    @Mapping(target = "passwordHash", ignore = true)
    void updateEntityFromRequest(UsuarioRequest request, @MappingTarget Usuario entity);
}
