package com.ceramax.api.repository.acceso;

import com.ceramax.api.model.acceso.Cliente;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByCodigo(String codigo);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
    boolean existsByNumeroDocumentoAndTipoDocumento(String numeroDocumento, String tipoDocumento);
    boolean existsByNumeroDocumentoAndTipoDocumentoAndIdNot(String numeroDocumento, String tipoDocumento, UUID id);
}
