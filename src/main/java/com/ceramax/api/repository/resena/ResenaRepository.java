package com.ceramax.api.repository.resena;

import com.ceramax.api.model.resena.Resena;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResenaRepository extends JpaRepository<Resena, UUID> {

    Optional<Resena> findByIdAndIdClienteAndActivoTrue(UUID id, UUID idCliente);

    Optional<Resena> findByIdClienteAndIdProducto(UUID idCliente, UUID idProducto);

    @Query(
        value = """
            SELECT id,
                   id_producto AS "productoId",
                   producto_codigo AS "productoCodigo",
                   producto_nombre AS "productoNombre",
                   id_cliente AS "clienteId",
                   cliente_nombre AS "clienteNombre",
                   id_pedido AS "pedidoId",
                   calificacion,
                   comentario,
                   created_at AS "creadoEn"
            FROM vw_resenas_producto
            WHERE id_producto = :productoId
            ORDER BY created_at DESC, id DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM vw_resenas_producto
            WHERE id_producto = :productoId
            """,
        nativeQuery = true
    )
    Page<ResenaPublicaProjection> listarPublicas(
        @Param("productoId") UUID productoId,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT id_producto AS "productoId",
                   cantidad_resenas AS "cantidadResenas",
                   calificacion_promedio AS "calificacionPromedio",
                   cinco_estrellas AS "cincoEstrellas",
                   cuatro_estrellas AS "cuatroEstrellas",
                   tres_estrellas AS "tresEstrellas",
                   dos_estrellas AS "dosEstrellas",
                   una_estrella AS "unaEstrella"
            FROM vw_resumen_resenas_producto
            WHERE id_producto = :productoId
            """,
        nativeQuery = true
    )
    Optional<ResumenResenasProjection> obtenerResumen(@Param("productoId") UUID productoId);
}
