package com.ceramax.api.repository.soporte;

import com.ceramax.api.model.soporte.ChatMensajeInternoAdjunto;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMensajeInternoAdjuntoRepository
    extends JpaRepository<ChatMensajeInternoAdjunto, UUID> {

    List<ChatMensajeInternoAdjunto> findByIdMensajeInOrderByCreatedAtAsc(Collection<UUID> idsMensaje);
}
