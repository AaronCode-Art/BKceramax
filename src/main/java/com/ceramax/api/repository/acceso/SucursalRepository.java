package com.ceramax.api.repository.acceso;

import com.ceramax.api.model.acceso.Sucursal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SucursalRepository extends JpaRepository<Sucursal, UUID> {
    Optional<Sucursal> findByCodigo(String codigo);
    Optional<Sucursal> findByActivoTrue();
}
