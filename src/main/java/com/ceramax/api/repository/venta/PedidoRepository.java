package com.ceramax.api.repository.venta;

import com.ceramax.api.model.venta.Pedido;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
    @EntityGraph(attributePaths = "estado")
    List<Pedido> findByCliente_IdOrderByCreatedAtDesc(UUID clienteId);

    @EntityGraph(attributePaths = "estado")
    Page<Pedido> findByCliente_IdOrderByCreatedAtDesc(UUID clienteId, Pageable pageable);

    @EntityGraph(attributePaths = "estado")
    List<Pedido> findByDelivery_IdAndEstado_CodigoInOrderByCreatedAtDesc(
        UUID deliveryId,
        List<String> estados
    );

    @EntityGraph(attributePaths = "estado")
    Page<Pedido> findByDelivery_IdAndEstado_CodigoInOrderByCreatedAtDesc(
        UUID deliveryId,
        List<String> estados,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "estado")
    List<Pedido> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "estado")
    Page<Pedido> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "estado")
    List<Pedido> findByEstado_CodigoInOrderByCreatedAtDesc(List<String> estados);

    @Query(
        value = """
            SELECT a.id AS id,
                   a.datos_anteriores->>'estado' AS "estadoAnteriorCodigo",
                   a.datos_nuevos->>'estado' AS "estadoCodigo",
                   a.accion AS accion,
                   a.id_usuario AS "usuarioId",
                   COALESCE(NULLIF(BTRIM(CONCAT_WS(' ', u.nombres, u.apellidos)), ''), 'Sistema') AS "usuarioNombre",
                   a.created_at AS "creadoEn"
            FROM auditoria a
            LEFT JOIN usuarios u ON u.id = a.id_usuario
            WHERE a.tabla_afectada = 'pedido'
              AND a.id_registro = :pedidoId
              AND jsonb_exists(a.datos_nuevos, 'estado')
              AND a.datos_anteriores->>'estado' IS DISTINCT FROM a.datos_nuevos->>'estado'
            ORDER BY a.created_at ASC, a.id ASC
            """,
        nativeQuery = true
    )
    List<PedidoEstadoHistorialProjection> findHistorialEstados(@Param("pedidoId") UUID pedidoId);

    @Query("SELECT p FROM Pedido p JOIN FETCH p.estado WHERE p.id = :pedidoId")
    Optional<Pedido> findByIdWithEstado(@Param("pedidoId") UUID pedidoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p JOIN FETCH p.estado WHERE p.id = :pedidoId")
    Optional<Pedido> findByIdForUpdate(@Param("pedidoId") UUID pedidoId);

    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM pedido p
                JOIN estadoenvio e ON e.id = p.id_estado
                JOIN detallepedido d ON d.id_pedido = p.id
                WHERE p.id = :pedidoId
                  AND p.id_cliente = :clienteId
                  AND e.codigo = 'COMPLETADA'
                  AND d.id_producto = :productoId
                  AND d.cantidad > 0
            )
            """,
        nativeQuery = true
    )
    boolean existeCompraCompletadaConProducto(
        @Param("pedidoId") UUID pedidoId,
        @Param("clienteId") UUID clienteId,
        @Param("productoId") UUID productoId
    );
}
