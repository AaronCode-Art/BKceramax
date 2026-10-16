package com.ceramax.api.mappers.inventario;

import com.ceramax.api.dto.response.AlmacenResponse;
import com.ceramax.api.model.inventario.Almacen;
import org.springframework.stereotype.Component;

@Component
public class AlmacenMapper {

    public AlmacenResponse toResponse(Almacen almacen) {
        return new AlmacenResponse(
            almacen.getId(),
            almacen.getCodigo(),
            almacen.getNombre(),
            almacen.getSucursal().getId(),
            almacen.getSucursal().getNombre(),
            almacen.getActivo(),
            almacen.getCreatedAt(),
            almacen.getUpdatedAt()
        );
    }
}
