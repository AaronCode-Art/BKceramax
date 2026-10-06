package com.ceramax.api.repository.acceso;

import com.ceramax.api.model.acceso.Usuario;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findWithRolByEmail(String email);
    Optional<Usuario> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
    boolean existsByNumeroDocumentoAndTipoDocumento(String numeroDocumento, String tipoDocumento);
    boolean existsByNumeroDocumentoAndTipoDocumentoAndIdNot(String numeroDocumento, String tipoDocumento, UUID id);
    List<Usuario> findByRol_CodigoAndActivoTrueOrderByApellidosAscNombresAsc(String rolCodigo);
}
