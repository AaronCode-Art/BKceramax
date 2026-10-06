package com.ceramax.api.security;

import com.ceramax.api.service.soporte.ChatService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Set<String> DESTINOS_LOGISTICA = Set.of(
            "/topic/logistica/alertas",
            "/topic/logistica/traslados");
    private static final String DESTINO_PEDIDOS_USUARIO = "/user/queue/pedidos";
    private static final Pattern SEND_CHAT_DESTINATION = Pattern.compile(
            "^/app/chats/(soporte|interno)/([0-9a-fA-F-]{36})/mensajes$");
    private static final Pattern SUBSCRIBE_CHAT_DESTINATION = Pattern.compile(
            "^/topic/(chat|chat-interno)/([0-9a-fA-F-]{36})$");

    private final JwtService jwtService;
    private final ChatService chatService;

    public WebSocketAuthInterceptor(JwtService jwtService, ChatService chatService) {
        this.jwtService = jwtService;
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders == null || authHeaders.isEmpty()) {
                throw new IllegalArgumentException("JWT requerido para conectarse al WebSocket");
            }

            String bearer = authHeaders.get(0);
            if (bearer == null || !bearer.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Token inválido");
            }

            String token = bearer.substring(7);
            if (!jwtService.isTokenValid(token)) {
                throw new IllegalArgumentException("Token JWT inválido");
            }

            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            if (email == null || email.isBlank() || role == null || role.isBlank()) {
                throw new IllegalArgumentException("El token JWT no contiene identidad y rol");
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            accessor.setUser(authentication);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            Matcher matcher = SEND_CHAT_DESTINATION.matcher(
                    accessor.getDestination() == null ? "" : accessor.getDestination());
            if (!matcher.matches()) {
                throw new MessageDeliveryException(message, "Destino de envío WebSocket no permitido");
            }
            validarAccesoChat(matcher.group(1), matcher.group(2), accessor.getUser(), message);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && accessor.getDestination() != null
                && (accessor.getDestination().startsWith("/topic/chat/")
                        || accessor.getDestination().startsWith("/topic/chat-interno/"))) {
            Matcher matcher = SUBSCRIBE_CHAT_DESTINATION.matcher(accessor.getDestination());
            if (!matcher.matches() || accessor.getSubscriptionId() == null) {
                throw new MessageDeliveryException(message, "Suscripción WebSocket no permitida");
            }
            String tipo = "chat".equals(matcher.group(1)) ? "soporte" : "interno";
            validarAccesoChat(tipo, matcher.group(2), accessor.getUser(), message);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && accessor.getDestination() != null
                && accessor.getDestination().startsWith("/topic/logistica/")) {
            if (!DESTINOS_LOGISTICA.contains(accessor.getDestination())
                    || accessor.getSubscriptionId() == null) {
                throw new MessageDeliveryException(message, "Suscripción WebSocket no permitida");
            }
            validarAccesoLogistica(accessor.getUser(), message);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && accessor.getDestination() != null
                && accessor.getDestination().startsWith("/topic/")) {
            throw new MessageDeliveryException(message, "Suscripción WebSocket no permitida");
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && accessor.getDestination() != null) {
            if (!DESTINO_PEDIDOS_USUARIO.equals(accessor.getDestination())
                    || accessor.getSubscriptionId() == null
                    || !(accessor.getUser() instanceof Authentication authentication)
                    || !authentication.isAuthenticated()) {
                throw new MessageDeliveryException(message, "Suscripción WebSocket no permitida");
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            throw new MessageDeliveryException(message, "Suscripción WebSocket no permitida");
        } else if (SimpMessageType.MESSAGE.equals(accessor.getMessageType())) {
            throw new MessageDeliveryException(message, "Tipo de mensaje WebSocket no permitido");
        }

        return message;
    }

    private void validarAccesoChat(
            String tipo,
            String identificador,
            java.security.Principal principal,
            Message<?> message) {
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new MessageDeliveryException(message, "JWT requerido para acceder al chat");
        }
        UUID chatId;
        try {
            chatId = UUID.fromString(identificador);
        } catch (IllegalArgumentException ex) {
            throw new MessageDeliveryException(message, "Identificador de chat inválido");
        }
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
        if (!chatService.puedeAcceder(tipo, chatId, authentication.getName(), role)) {
            throw new MessageDeliveryException(message, "No tienes acceso a este chat");
        }
    }

    private void validarAccesoLogistica(java.security.Principal principal, Message<?> message) {
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new MessageDeliveryException(message, "JWT requerido para suscribirse a logística");
        }
        boolean autorizado = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("ROLE_LOGISTICA"));
        if (!autorizado) {
            throw new MessageDeliveryException(message, "No tienes acceso a las notificaciones de logística");
        }
    }
}
