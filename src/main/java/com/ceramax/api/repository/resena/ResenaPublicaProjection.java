package com.ceramax.api.repository.resena;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ResenaPublicaProjection {
    UUID getId();
    UUID getProductoId();
    String getProductoCodigo();
    String getProductoNombre();
    UUID getClienteId();
    String getClienteNombre();
    UUID getPedidoId();
    Short getCalificacion();
    String getComentario();
    OffsetDateTime getCreadoEn();
}
