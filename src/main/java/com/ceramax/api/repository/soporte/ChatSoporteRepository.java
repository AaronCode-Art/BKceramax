package com.ceramax.api.repository.soporte;

import com.ceramax.api.model.soporte.ChatSoporte;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatSoporteRepository extends JpaRepository<ChatSoporte, UUID> {

    Page<ChatSoporte> findByIdClienteOrderByUpdatedAtDescIdDesc(UUID idCliente, Pageable pageable);

    @Query(
        value = """
            select c from ChatSoporte c
            where c.idUsuarioCreador = :usuarioId
               or c.idUsuarioAsignado = :usuarioId
               or c.idUsuarioAsignado is null
            order by c.updatedAt desc, c.id desc
            """,
        countQuery = """
            select count(c) from ChatSoporte c
            where c.idUsuarioCreador = :usuarioId
               or c.idUsuarioAsignado = :usuarioId
               or c.idUsuarioAsignado is null
            """
    )
    Page<ChatSoporte> findVisibleForStaff(@Param("usuarioId") UUID usuarioId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChatSoporte c where c.id = :id")
    Optional<ChatSoporte> findByIdForUpdate(@Param("id") UUID id);
}
