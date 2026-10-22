package com.ceramax.api.repository.venta;

import com.ceramax.api.model.venta.Cupon;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuponRepository extends JpaRepository<Cupon, UUID> {

    List<Cupon> findAllByOrderByFechaInicioDesc();

    Optional<Cupon> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cupon c WHERE c.codigo = :codigo")
    Optional<Cupon> findByCodigoForUpdate(@Param("codigo") String codigo);

    @Query("""
        SELECT c FROM Cupon c
        WHERE c.activo = true
          AND c.cliente IS NULL
          AND c.fechaInicio <= CURRENT_TIMESTAMP
          AND (c.fechaFin IS NULL OR c.fechaFin >= CURRENT_TIMESTAMP)
          AND (
            c.usosMaximos IS NULL
            OR (SELECT COUNT(p) FROM Pedido p WHERE p.cupon = c) < c.usosMaximos
          )
        ORDER BY c.fechaFin ASC NULLS LAST
        """)
    List<Cupon> findCurrentAnnouncements();
}
