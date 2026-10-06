package com.ceramax.api.security;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.service.soporte.ChatService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class WebSocketAuthInterceptorTest {

    private final JwtService jwtService = org.mockito.Mockito.mock(JwtService.class);
    private final ChatService chatService = org.mockito.Mockito.mock(ChatService.class);
    private final MessageChannel channel = org.mockito.Mockito.mock(MessageChannel.class);
    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtService, chatService);
    }

    @Test
    void permiteEnviarMensajeSoloAlParticipanteDelChat() {
        UUID chatId = UUID.randomUUID();
        Message<?> message = message(
            StompCommand.SEND,
            "/app/chats/soporte/" + chatId + "/mensajes",
            "session-1",
            authenticatedCustomer()
        );
        when(chatService.puedeAcceder("soporte", chatId, "cliente@example.com", "CLIENTE"))
            .thenReturn(true);

        assertSame(message, interceptor.preSend(message, channel));
    }

    @Test
    void rechazaEnvioDeMensajeSiElUsuarioNoPerteneceAlChat() {
        UUID chatId = UUID.randomUUID();
        Message<?> message = message(
            StompCommand.SEND,
            "/app/chats/soporte/" + chatId + "/mensajes",
            "session-1",
            authenticatedCustomer()
        );
        when(chatService.puedeAcceder("soporte", chatId, "cliente@example.com", "CLIENTE"))
            .thenReturn(false);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void rechazaSuscripcionAChatAjeno() {
        UUID chatId = UUID.randomUUID();
        Message<?> message = message(
            StompCommand.SUBSCRIBE,
            "/topic/chat-interno/" + chatId,
            "session-1",
            authenticatedCustomer()
        );
        when(chatService.puedeAcceder("interno", chatId, "cliente@example.com", "CLIENTE"))
            .thenReturn(false);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void rechazaEnviosAEndpointsQueNoSeanDeChat() {
        Message<?> message = message(
            StompCommand.SEND,
            "/app/administracion",
            "session-1",
            authenticatedCustomer()
        );

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void permiteSuscribirAlertasDeLogisticaSoloAlRolAutorizado() {
        Message<?> message = message(
            StompCommand.SUBSCRIBE,
            "/topic/logistica/alertas",
            "subscription-1",
            authenticatedAs("LOGISTICA")
        );

        assertSame(message, interceptor.preSend(message, channel));
    }

    @Test
    void rechazaSuscripcionDeDeliveryAAlertasInternasDeLogistica() {
        Message<?> message = message(
            StompCommand.SUBSCRIBE,
            "/topic/logistica/alertas",
            "subscription-1",
            authenticatedAs("DELIVERY")
        );

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void validaJwtAlConectar() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer jwt-valido");
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(jwtService.isTokenValid("jwt-valido")).thenReturn(true);
        when(jwtService.extractEmail("jwt-valido")).thenReturn("cliente@example.com");
        when(jwtService.extractRole("jwt-valido")).thenReturn("CLIENTE");

        Message<?> authenticatedMessage = interceptor.preSend(message, channel);
        org.junit.jupiter.api.Assertions.assertNotSame(message, authenticatedMessage);
        assertNotNull(StompHeaderAccessor.wrap(authenticatedMessage).getUser());
        assertEquals("cliente@example.com", StompHeaderAccessor.wrap(authenticatedMessage).getUser().getName());
        verify(jwtService).isTokenValid("jwt-valido");
    }

    @Test
    void rechazaConexionSinJwtValido() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer invalido");
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(jwtService.isTokenValid("invalido")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, channel));
    }

    private Message<?> message(
        StompCommand command,
        String destination,
        String subscriptionId,
        UsernamePasswordAuthenticationToken principal
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private UsernamePasswordAuthenticationToken authenticatedCustomer() {
        return authenticatedAs("CLIENTE");
    }

    private UsernamePasswordAuthenticationToken authenticatedAs(String role) {
        return new UsernamePasswordAuthenticationToken(
            "cliente@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
