package com.ceramax.api.mappers.acceso;

import com.ceramax.api.dto.request.ClienteRequest;
import com.ceramax.api.dto.response.ClienteResponse;
import com.ceramax.api.model.acceso.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ClienteMapper {
    Cliente toEntity(ClienteRequest request);

    @Mapping(target = "codigo", source = "codigo")
    ClienteResponse toResponse(Cliente entity);

    void updateEntityFromRequest(ClienteRequest request, @MappingTarget Cliente entity);
}
