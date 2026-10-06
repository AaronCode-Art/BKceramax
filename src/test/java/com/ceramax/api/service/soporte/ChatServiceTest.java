package com.ceramax.api.service.soporte;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.request.EnviarMensajeChatRequest;
import com.ceramax.api.exception.ForbiddenException;
import com.ceramax.api.model.acceso.Rol;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.model.soporte.ChatInterno;
import com.ceramax.api.model.soporte.ChatMensajeInterno;
import com.ceramax.api.observer.event.MensajeChatEvent;
import com.ceramax.api.repository.acceso.ClienteRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import com.ceramax.api.repository.soporte.ChatInternoRepository;
import com.ceramax.api.repository.soporte.ChatMensajeInternoRepository;
import com.ceramax.api.repository.soporte.ChatSoporteRepository;
import com.ceramax.api.repository.soporte.MensajeSoporteRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

class ChatServiceTest {

    private final ChatSoporteRepository chatSoporteRepository = org.mockito.Mockito.mock(ChatSoporteRepository.class);
    private final MensajeSoporteRepository mensajeSoporteRepository =
        org.mockito.Mockito.mock(MensajeSoporteRepository.class);
    private final ChatInternoRepository chatInternoRepository = org.mockito.Mockito.mock(ChatInternoRepository.class);
    private final ChatMensajeInternoRepository chatMensajeInternoRepository =
        org.mockito.Mockito.mock(ChatMensajeInternoRepository.class);
    private final ClienteRepository clienteRepository = org.mockito.Mockito.mock(ClienteRepository.class);
    private final UsuarioRepository usuarioRepository = org.mockito.Mockito.mock(UsuarioRepository.class);
    private final ApplicationEventPublisher eventPublisher =
        org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final EntityManager entityManager = org.mockito.Mockito.mock(EntityManager.class);
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
            chatSoporteRepository,
            mensajeSoporteRepository,
            chatInternoRepository,
            chatMensajeInternoRepository,
            clienteRepository,
            usuarioRepository,
            eventPublisher,
            entityManager
        );
    }

    @Test
    void persisteMensajeInternoAntesDePublicarEvento() {
        UUID usuarioId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        Usuario usuario = usuarioInterno(usuarioId);
        ChatInterno chat = new ChatInterno();
        chat.setId(chatId);
        chat.setIdUsuarioCreador(usuarioId);
        chat.setEstado("ABIERTO");
        when(chatInternoRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(usuarioRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(usuario));
        when(chatMensajeInternoRepository.saveAndFlush(any(ChatMensajeInterno.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        chatService.enviarMensajeInterno(
            chatId,
            "staff@example.com",
            new EnviarMensajeChatRequest("  mensaje de prueba  ")
        );

        InOrder inOrder = inOrder(chatMensajeInternoRepository, eventPublisher);
        inOrder.verify(chatMensajeInternoRepository).saveAndFlush(any(ChatMensajeInterno.class));
        inOrder.verify(eventPublisher).publishEvent(any(MensajeChatEvent.class));
    }

    @Test
    void noPersisteMensajeInternoDeUsuarioAjeno() {
        UUID chatId = UUID.randomUUID();
        UUID creadorId = UUID.randomUUID();
        Usuario usuario = usuarioInterno(UUID.randomUUID());
        ChatInterno chat = new ChatInterno();
        chat.setId(chatId);
        chat.setIdUsuarioCreador(creadorId);
        chat.setEstado("ABIERTO");
        when(chatInternoRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(usuarioRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(usuario));

        assertThrows(ForbiddenException.class, () -> chatService.enviarMensajeInterno(
            chatId,
            "staff@example.com",
            new EnviarMensajeChatRequest("mensaje")
        ));

        verify(chatMensajeInternoRepository, never()).saveAndFlush(any(ChatMensajeInterno.class));
        verify(eventPublisher, never()).publishEvent(any(MensajeChatEvent.class));
    }

    private Usuario usuarioInterno(UUID id) {
        Rol rol = new Rol();
        rol.setCodigo("ADMIN");
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail("staff@example.com");
        usuario.setActivo(true);
        usuario.setRol(rol);
        return usuario;
    }
}
