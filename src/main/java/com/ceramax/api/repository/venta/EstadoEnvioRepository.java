package com.ceramax.api.repository.venta;

import com.ceramax.api.model.venta.EstadoEnvio;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstadoEnvioRepository extends JpaRepository<EstadoEnvio, UUID> {
    Optional<EstadoEnvio> findByCodigo(String codigo);

    @Query(
        value = """
            SELECT DISTINCT destino.codigo
            FROM transicion_estado t
            JOIN estadoenvio origen ON origen.id = t.id_estado_origen
            JOIN estadoenvio destino ON destino.id = t.id_estado_destino
            WHERE origen.id = :origenId
              AND (t.tipo_entrega = :tipoEntrega OR t.tipo_entrega IS NULL)
            ORDER BY destino.codigo
            """,
        nativeQuery = true
    )
    List<String> listarCodigosDestinoTransiciones(
        @Param("origenId") UUID origenId,
        @Param("tipoEntrega") String tipoEntrega
    );

    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM transicion_estado t
                JOIN estadoenvio origen ON origen.id=t.id_estado_origen
                JOIN estadoenvio destino ON destino.id=t.id_estado_destino
                WHERE origen.id=:origenId
                  AND destino.id=:destinoId
                  AND (t.tipo_entrega=:tipoEntrega OR t.tipo_entrega IS NULL)
            )
            """,
        nativeQuery = true
    )
    boolean existeTransicion(
        @Param("origenId") UUID origenId,
        @Param("destinoId") UUID destinoId,
        @Param("tipoEntrega") String tipoEntrega
    );
}
