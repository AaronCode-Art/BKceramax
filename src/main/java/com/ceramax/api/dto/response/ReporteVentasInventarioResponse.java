package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReporteVentasInventarioResponse(
    LocalDate desde,
    LocalDate hasta,
    OffsetDateTime generadoEn,
    Long cantidadVentas,
    Long ventasWeb,
    Long ventasPresenciales,
    BigDecimal ingresosTotales,
    List<ProductoMasVendidoResponse> productosMasVendidos,
    List<InventarioReporteResponse> inventarioActual
) {
    public record ProductoMasVendidoResponse(
        UUID productoId,
        String codigo,
        String nombre,
        Long unidadesVendidas,
        BigDecimal importeVendido
    ) {}

    public record InventarioReporteResponse(
        UUID almacenId,
        String almacenCodigo,
        String almacenNombre,
        UUID productoId,
        String productoCodigo,
        String productoNombre,
        String categoriaNombre,
        Integer stockFisico,
        Integer stockReservado,
        Integer stockDisponible,
        Integer stockMinimo,
        Boolean stockBajo
    ) {}
}
