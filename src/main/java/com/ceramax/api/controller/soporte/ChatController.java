package com.ceramax.api.controller.soporte;

import com.ceramax.api.dto.request.CrearChatInternoRequest;
import com.ceramax.api.dto.request.CrearChatSoporteRequest;
import com.ceramax.api.dto.request.CrearChatSoporteParaClienteRequest;
import com.ceramax.api.dto.response.ChatResponse;
import com.ceramax.api.dto.response.MensajeChatResponse;
import com.ceramax.api.service.soporte.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/soporte")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ChatResponse> crearChatSoporte(
        Authentication authentication,
        @Valid @RequestBody CrearChatSoporteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(chatService.crearChatSoporte(authentication.getName(), request));
    }

    @PostMapping("/soporte/cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<ChatResponse> crearChatSoporteParaCliente(
        Authentication authentication,
        @Valid @RequestBody CrearChatSoporteParaClienteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(chatService.crearChatSoporteParaCliente(authentication.getName(), request));
    }

    @GetMapping("/soporte")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY', 'CLIENTE')")
    public ResponseEntity<List<ChatResponse>> listarChatsSoporte(Authentication authentication) {
        return ResponseEntity.ok(chatService.listarChatsSoporte(authentication.getName(), obtenerRol(authentication)));
    }

    @GetMapping("/soporte/{chatId}/mensajes")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY', 'CLIENTE')")
    public ResponseEntity<List<MensajeChatResponse>> listarMensajesSoporte(
        Authentication authentication,
        @PathVariable UUID chatId
    ) {
        return ResponseEntity.ok(chatService.listarMensajesSoporte(
            chatId, authentication.getName(), obtenerRol(authentication)
        ));
    }

    @PostMapping("/interno")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<ChatResponse> crearChatInterno(
        Authentication authentication,
        @Valid @RequestBody CrearChatInternoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(chatService.crearChatInterno(authentication.getName(), request));
    }

    @GetMapping("/interno")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<List<ChatResponse>> listarChatsInternos(Authentication authentication) {
        return ResponseEntity.ok(chatService.listarChatsInternos(authentication.getName()));
    }

    @GetMapping("/interno/{chatId}/mensajes")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<List<MensajeChatResponse>> listarMensajesInternos(
        Authentication authentication,
        @PathVariable UUID chatId
    ) {
        return ResponseEntity.ok(chatService.listarMensajesInternos(chatId, authentication.getName()));
    }

    private String obtenerRol(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring("ROLE_".length()))
            .findFirst()
            .orElse("");
    }
}
