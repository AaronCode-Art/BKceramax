package com.ceramax.api.mappers.soporte;

import com.ceramax.api.dto.response.ChatAdjuntoResponse;
import com.ceramax.api.dto.response.ChatInternoMiembroResponse;
import com.ceramax.api.dto.response.ChatResponse;
import com.ceramax.api.dto.response.MensajeChatResponse;
import com.ceramax.api.model.soporte.ChatInterno;
import com.ceramax.api.model.soporte.ChatMensajeInterno;
import com.ceramax.api.model.soporte.ChatMensajeInternoAdjunto;
import com.ceramax.api.model.soporte.ChatSoporte;
import com.ceramax.api.model.soporte.MensajeSoporte;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ChatResponse toSupportChat(ChatSoporte chat) {
        return new ChatResponse(
            chat.getId(), chat.getCodigo(), "soporte", chat.getIdCliente(), chat.getIdPedido(),
            chat.getIdUsuarioCreador(), chat.getIdUsuarioAsignado(), null, chat.getAsunto(),
            chat.getEstado(), chat.getCreatedAt(), chat.getUpdatedAt(), List.of()
        );
    }

    public ChatResponse toInternalChat(ChatInterno chat, List<ChatInternoMiembroResponse> miembros) {
        return new ChatResponse(
            chat.getId(), chat.getCodigo(), "interno", null, null, chat.getIdUsuarioCreador(),
            null, chat.getIdUsuarioDestino(), chat.getAsunto(), chat.getEstado(),
            chat.getCreatedAt(), chat.getUpdatedAt(), miembros
        );
    }

    public MensajeChatResponse toSupportMessage(MensajeSoporte mensaje) {
        return new MensajeChatResponse(
            mensaje.getId(), mensaje.getIdChat(),
            mensaje.getIdCliente() == null ? mensaje.getIdUsuario() : mensaje.getIdCliente(),
            mensaje.getTipoRemitente(), mensaje.getContenido(), mensaje.getLeido(), mensaje.getCreatedAt(),
            List.of()
        );
    }

    public MensajeChatResponse toInternalMessage(
        ChatMensajeInterno mensaje,
        List<ChatAdjuntoResponse> adjuntos
    ) {
        return new MensajeChatResponse(
            mensaje.getId(), mensaje.getIdChat(), mensaje.getIdUsuario(), "PERSONAL",
            mensaje.getContenido(), mensaje.getLeido(), mensaje.getCreatedAt(), adjuntos
        );
    }

    public ChatAdjuntoResponse toAttachment(ChatMensajeInternoAdjunto adjunto) {
        return new ChatAdjuntoResponse(
            adjunto.getId(), adjunto.getNombreArchivo(), adjunto.getUrl(),
            adjunto.getTipoContenido(), adjunto.getTamanoBytes(), adjunto.getCreatedAt()
        );
    }
}
