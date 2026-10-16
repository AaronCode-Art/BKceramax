package com.ceramax.api.repository.soporte;

import com.ceramax.api.dto.response.ChatInternoMiembroResponse;
import com.ceramax.api.model.soporte.ChatInternoMiembro;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatInternoMiembroRepository extends JpaRepository<ChatInternoMiembro, UUID> {

    List<ChatInternoMiembro> findByIdChatIn(Collection<UUID> chatIds);

    boolean existsByIdChatAndIdUsuario(UUID chatId, UUID usuarioId);

    @Query("""
        select new com.ceramax.api.dto.response.ChatInternoMiembroResponse(
            miembro.idChat, miembro.idUsuario, usuario.nombres, usuario.apellidos, usuario.email, rol.codigo
        )
        from ChatInternoMiembro miembro
        join Usuario usuario on usuario.id = miembro.idUsuario
        join usuario.rol rol
        where miembro.idChat = :chatId
        order by usuario.apellidos, usuario.nombres
        """)
    List<ChatInternoMiembroResponse> listarMiembros(@Param("chatId") UUID chatId);

    @Query("""
        select new com.ceramax.api.dto.response.ChatInternoMiembroResponse(
            miembro.idChat, miembro.idUsuario, usuario.nombres, usuario.apellidos, usuario.email, rol.codigo
        )
        from ChatInternoMiembro miembro
        join Usuario usuario on usuario.id = miembro.idUsuario
        join usuario.rol rol
        where miembro.idChat in :chatIds
        order by usuario.apellidos, usuario.nombres
        """)
    List<ChatInternoMiembroResponse> listarMiembrosPorChats(@Param("chatIds") Collection<UUID> chatIds);
}
