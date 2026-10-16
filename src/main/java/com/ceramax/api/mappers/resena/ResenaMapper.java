package com.ceramax.api.mappers.resena;

import com.ceramax.api.dto.response.ResenaImagenResponse;
import com.ceramax.api.dto.response.ResenaResponse;
import com.ceramax.api.dto.response.ResumenResenasResponse;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.catalogo.Producto;
import com.ceramax.api.model.resena.Resena;
import com.ceramax.api.model.resena.ResenaImagen;
import com.ceramax.api.repository.resena.ResenaPublicaProjection;
import com.ceramax.api.repository.resena.ResumenResenasProjection;
import java.util.List;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class ResenaMapper {

    public ResenaResponse toResponse(
        Resena resena,
        Producto producto,
        Cliente cliente,
        List<ResenaImagenResponse> imagenes
    ) {
        return new ResenaResponse(
            resena.getId(),
            resena.getIdProducto(),
            producto.getCodigo(),
            producto.getNombre(),
            resena.getIdCliente(),
            cliente.getNombres() + " " + cliente.getApellidos(),
            resena.getIdPedido(),
            resena.getCalificacion(),
            resena.getComentario(),
            resena.getCreatedAt(),
            imagenes
        );
    }

    public ResenaImagenResponse toImageResponse(ResenaImagen imagen) {
        return new ResenaImagenResponse(imagen.getId(), imagen.getUrl(), imagen.getOrden());
    }

    public ResenaResponse toPublicResponse(
        ResenaPublicaProjection resena,
        List<ResenaImagenResponse> imagenes
    ) {
        return new ResenaResponse(
            resena.getId(),
            resena.getProductoId(),
            resena.getProductoCodigo(),
            resena.getProductoNombre(),
            resena.getClienteId(),
            resena.getClienteNombre(),
            resena.getPedidoId(),
            resena.getCalificacion(),
            resena.getComentario(),
            toOffsetDateTime(resena.getCreadoEn()),
            imagenes
        );
    }

    private OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    public ResumenResenasResponse toSummary(ResumenResenasProjection resumen) {
        return new ResumenResenasResponse(
            resumen.getProductoId(),
            resumen.getCantidadResenas(),
            resumen.getCalificacionPromedio(),
            resumen.getCincoEstrellas(),
            resumen.getCuatroEstrellas(),
            resumen.getTresEstrellas(),
            resumen.getDosEstrellas(),
            resumen.getUnaEstrella()
        );
    }
}
