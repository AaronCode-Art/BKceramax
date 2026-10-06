package com.ceramax.api.controller.soporte;

import com.ceramax.api.dto.request.EnviarMensajeChatRequest;
import com.ceramax.api.dto.response.MensajeChatResponse;
import com.ceramax.api.service.soporte.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    public ChatWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chats/soporte/{chatId}/mensajes")
    public void enviarMensajeSoporte(
        @DestinationVariable UUID chatId,
        @Valid EnviarMensajeChatRequest request,
        Principal principal
    ) {
        chatService.enviarMensajeSoporte(chatId, principal.getName(), obtenerRol(principal), request);
    }

    @MessageMapping("/chats/interno/{chatId}/mensajes")
    public void enviarMensajeInterno(
        @DestinationVariable UUID chatId,
        @Valid EnviarMensajeChatRequest request,
        Principal principal
    ) {
        chatService.enviarMensajeInterno(chatId, principal.getName(), request);
    }

    private String obtenerRol(Principal principal) {
        if (principal instanceof org.springframework.security.core.Authentication authentication) {
            return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
        }
        return "";
    }
}
