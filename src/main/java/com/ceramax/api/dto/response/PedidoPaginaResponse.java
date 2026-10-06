package com.ceramax.api.dto.response;

import java.util.List;

public record PedidoPaginaResponse(
    List<PedidoResumenResponse> contenido,
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas,
    boolean primera,
    boolean ultima,
    boolean haySiguiente
) {}
