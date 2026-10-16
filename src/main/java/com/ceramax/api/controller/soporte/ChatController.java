package com.ceramax.api.controller.soporte;

import com.ceramax.api.dto.request.CrearChatInternoRequest;
import com.ceramax.api.dto.request.CrearChatSoporteRequest;
import com.ceramax.api.dto.request.CrearChatSoporteParaClienteRequest;
import com.ceramax.api.dto.request.ActualizarNombreChatInternoRequest;
import com.ceramax.api.dto.request.EnviarMensajeChatRequest;
import com.ceramax.api.dto.response.ChatResponse;
import com.ceramax.api.dto.response.ChatInternoUsuarioResponse;
import com.ceramax.api.dto.response.MensajeChatResponse;
import com.ceramax.api.service.soporte.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
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
    public ResponseEntity<Page<ChatResponse>> listarChatsSoporte(
        Authentication authentication,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(chatService.listarChatsSoporte(
            authentication.getName(),
            obtenerRol(authentication),
            PageRequest.of(page, size)
        ));
    }

    @GetMapping("/soporte/{chatId}/mensajes")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY', 'CLIENTE')")
    public ResponseEntity<Page<MensajeChatResponse>> listarMensajesSoporte(
        Authentication authentication,
        @PathVariable UUID chatId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(chatService.listarMensajesSoporte(
            chatId,
            authentication.getName(),
            obtenerRol(authentication),
            PageRequest.of(page, size)
        ));
    }

    @PostMapping("/interno")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<ChatResponse> crearChatInterno(
        Authentication authentication,
        @Valid @RequestBody CrearChatInternoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(chatService.crearChatInterno(authentication.getName(), request));
    }

    @GetMapping("/interno/usuarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<List<ChatInternoUsuarioResponse>> listarUsuariosChatInterno(
        Authentication authentication
    ) {
        return ResponseEntity.ok(chatService.listarUsuariosChatInterno(authentication.getName()));
    }

    @GetMapping("/interno")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<Page<ChatResponse>> listarChatsInternos(
        Authentication authentication,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(chatService.listarChatsInternos(
            authentication.getName(),
            PageRequest.of(page, size)
        ));
    }

    @GetMapping("/interno/{chatId}/mensajes")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<Page<MensajeChatResponse>> listarMensajesInternos(
        Authentication authentication,
        @PathVariable UUID chatId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(chatService.listarMensajesInternos(
            chatId,
            authentication.getName(),
            PageRequest.of(page, size)
        ));
    }

    @PostMapping("/interno/{chatId}/mensajes")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<MensajeChatResponse> enviarMensajeInterno(
        Authentication authentication,
        @PathVariable UUID chatId,
        @Valid @RequestBody EnviarMensajeChatRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            chatService.enviarMensajeInterno(chatId, authentication.getName(), request)
        );
    }

    @PostMapping(
        value = "/interno/{chatId}/adjuntos",
        consumes = "multipart/form-data"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<MensajeChatResponse> enviarAdjuntoInterno(
        Authentication authentication,
        @PathVariable UUID chatId,
        @RequestParam("archivo") MultipartFile archivo,
        @RequestParam(required = false, defaultValue = "") String contenido
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            chatService.enviarAdjuntoInterno(chatId, authentication.getName(), contenido, archivo)
        );
    }

    @PatchMapping("/interno/{chatId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<ChatResponse> actualizarNombreChatInterno(
        Authentication authentication,
        @PathVariable UUID chatId,
        @Valid @RequestBody ActualizarNombreChatInternoRequest request
    ) {
        return ResponseEntity.ok(chatService.actualizarNombreChatInterno(
            chatId,
            authentication.getName(),
            request.asunto()
        ));
    }

    @DeleteMapping("/interno/{chatId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<Void> cerrarChatInterno(Authentication authentication, @PathVariable UUID chatId) {
        chatService.cerrarChatInterno(chatId, authentication.getName());
        return ResponseEntity.noContent().build();
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
