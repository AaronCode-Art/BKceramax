package com.ceramax.api.mappers.catalogo;

import com.ceramax.api.dto.response.CatalogoPublicoResponse;
import com.ceramax.api.dto.response.ProductoDetallePublicoResponse;
import com.ceramax.api.repository.catalogo.CatalogoPublicoProjection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CatalogoPublicoMapper {

    public CatalogoPublicoResponse toResponse(CatalogoPublicoProjection projection) {
        return new CatalogoPublicoResponse(
            projection.getIdProducto(),
            projection.getProductoCodigo(),
            projection.getProductoNombre(),
            projection.getCategoriaNombre(),
            projection.getDescripcion(),
            projection.getPrecio(),
            projection.getPrecioFinal(),
            projection.getDescuentoPorcentaje(),
            projection.getStockDisponible()
        );
    }

    public ProductoDetallePublicoResponse toDetail(
        CatalogoPublicoResponse catalogo,
        List<Map<String, Object>> especificaciones
    ) {
        return new ProductoDetallePublicoResponse(
            catalogo.id(),
            catalogo.codigo(),
            catalogo.nombre(),
            catalogo.categoria(),
            catalogo.descripcion(),
            especificaciones == null ? List.of() : especificaciones,
            catalogo.precio(),
            catalogo.precioFinal(),
            catalogo.descuentoPorcentaje(),
            catalogo.stockDisponible()
        );
    }
}
