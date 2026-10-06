package com.ceramax.api.repository.venta;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VentaOperacionRepository {

    private final JdbcTemplate jdbcTemplate;

    public VentaOperacionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID sucursalActivaId() {
        return jdbcTemplate.queryForObject("SELECT fn_sucursal_unica()", UUID.class);
    }

    public BigDecimal parametroNumerico(String clave) {
        return jdbcTemplate.queryForObject("SELECT fn_parametro(?)", BigDecimal.class, clave);
    }

    public VentaPresencialResultado registrarVentaPresencial(
        UUID vendedorId,
        List<ItemVenta> items,
        UUID clienteId,
        String tipoComprobante,
        String metodoPago,
        String ruc,
        String razonSocial
    ) {
        String itemsJson = items.stream()
            .map(item -> "{\"id_producto\":\"" + item.id_producto() + "\",\"cantidad\":" + item.cantidad() + "}")
            .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return jdbcTemplate.queryForObject(
            """
                SELECT id_pedido, codigo_pedido, tipo_resolucion, mensaje
                FROM registrar_venta_presencial(
                    ?::uuid, ?::jsonb, ?::uuid, ?::varchar, ?::varchar, ?::varchar, ?::varchar
                )
                """,
            (resultSet, rowNumber) -> new VentaPresencialResultado(
                resultSet.getObject("id_pedido", UUID.class),
                resultSet.getString("codigo_pedido"),
                resultSet.getString("tipo_resolucion"),
                resultSet.getString("mensaje")
            ),
            vendedorId,
            itemsJson,
            clienteId,
            tipoComprobante,
            metodoPago,
            ruc,
            razonSocial
        );
    }

    public boolean reservarStock(UUID pedidoId) {
        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject("SELECT reservar_stock_pedido(?::uuid)", Boolean.class, pedidoId)
        );
    }

    public boolean liberarReserva(UUID pedidoId) {
        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject("SELECT liberar_reserva_pedido(?::uuid)", Boolean.class, pedidoId)
        );
    }

    public boolean despacharReservas(UUID pedidoId, UUID usuarioId) {
        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                "SELECT despachar_pedido_reservado(?::uuid, ?::uuid)",
                Boolean.class,
                pedidoId,
                usuarioId
            )
        );
    }

    public record ItemVenta(UUID id_producto, int cantidad) {}

    public record VentaPresencialResultado(UUID pedidoId, String codigoPedido, String tipoResolucion, String mensaje) {}
}
