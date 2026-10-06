package com.ceramax.api.service.soporte;

import com.ceramax.api.dto.request.CrearChatInternoRequest;
import com.ceramax.api.dto.request.CrearChatSoporteRequest;
import com.ceramax.api.dto.request.CrearChatSoporteParaClienteRequest;
import com.ceramax.api.dto.request.EnviarMensajeChatRequest;
import com.ceramax.api.dto.response.ChatResponse;
import com.ceramax.api.dto.response.MensajeChatResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ForbiddenException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.model.soporte.ChatInterno;
import com.ceramax.api.model.soporte.ChatMensajeInterno;
import com.ceramax.api.model.soporte.ChatSoporte;
import com.ceramax.api.model.soporte.MensajeSoporte;
import com.ceramax.api.observer.event.MensajeChatEvent;
import com.ceramax.api.repository.acceso.ClienteRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import com.ceramax.api.repository.soporte.ChatInternoRepository;
import com.ceramax.api.repository.soporte.ChatMensajeInternoRepository;
import com.ceramax.api.repository.soporte.ChatSoporteRepository;
import com.ceramax.api.repository.soporte.MensajeSoporteRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private static final Set<String> ROLES_INTERNOS = Set.of("ADMIN", "JEFE_LOGISTICA", "LOGISTICA", "VENDEDOR", "DELIVERY");

    private final ChatSoporteRepository chatSoporteRepository;
    private final MensajeSoporteRepository mensajeSoporteRepository;
    private final ChatInternoRepository chatInternoRepository;
    private final ChatMensajeInternoRepository chatMensajeInternoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public ChatService(
        ChatSoporteRepository chatSoporteRepository,
        MensajeSoporteRepository mensajeSoporteRepository,
        ChatInternoRepository chatInternoRepository,
        ChatMensajeInternoRepository chatMensajeInternoRepository,
        ClienteRepository clienteRepository,
        UsuarioRepository usuarioRepository,
        ApplicationEventPublisher eventPublisher,
        EntityManager entityManager
    ) {
        this.chatSoporteRepository = chatSoporteRepository;
        this.mensajeSoporteRepository = mensajeSoporteRepository;
        this.chatInternoRepository = chatInternoRepository;
        this.chatMensajeInternoRepository = chatMensajeInternoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
    }

    @Transactional
    public ChatResponse crearChatSoporte(String email, CrearChatSoporteRequest request) {
        Cliente cliente = clienteActivo(email);
        ChatSoporte chat = new ChatSoporte();
        chat.setIdCliente(cliente.getId());
        chat.setAsunto(normalizarAsunto(request.asunto()));
        chatSoporteRepository.saveAndFlush(chat);
        entityManager.refresh(chat);
        return mapear(chat);
    }

    @Transactional
    public ChatResponse crearChatSoporteParaCliente(
        String email,
        CrearChatSoporteParaClienteRequest request
    ) {
        Usuario usuario = usuarioInterno(email);
        Cliente cliente = clienteRepository.findById(request.clienteId())
            .filter(this::esClienteActivo)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente activo no encontrado"));
        ChatSoporte chat = new ChatSoporte();
        chat.setIdCliente(cliente.getId());
        chat.setIdUsuarioCreador(usuario.getId());
        chat.setIdUsuarioAsignado(usuario.getId());
        chat.setAsunto(normalizarAsunto(request.asunto()));
        chatSoporteRepository.saveAndFlush(chat);
        entityManager.refresh(chat);
        return mapear(chat);
    }

    @Transactional
    public ChatResponse crearChatInterno(String email, CrearChatInternoRequest request) {
        Usuario creador = usuarioInterno(email);
        ChatInterno chat = new ChatInterno();
        chat.setIdUsuarioCreador(creador.getId());
        chat.setAsunto(normalizarAsunto(request.asunto()));
        if (request.usuarioDestinoId() != null) {
            Usuario destino = usuarioRepository.findById(request.usuarioDestinoId())
                .filter(this::esUsuarioInternoActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario interno destinatario no encontrado"));
            if (destino.getId().equals(creador.getId())) {
                throw new BusinessException("El destinatario debe ser otro usuario");
            }
            chat.setIdUsuarioDestino(destino.getId());
        }
        chatInternoRepository.saveAndFlush(chat);
        entityManager.refresh(chat);
        return mapear(chat);
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> listarChatsSoporte(String email, String role) {
        if ("CLIENTE".equals(role)) {
            UUID clienteId = clienteActivo(email).getId();
            return chatSoporteRepository.findByIdClienteOrderByUpdatedAtDesc(clienteId)
                .stream().map(this::mapear).toList();
        }
        Usuario usuario = usuarioInterno(email);
        return chatSoporteRepository.findVisibleForStaff(usuario.getId())
            .stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> listarChatsInternos(String email) {
        UUID usuarioId = usuarioInterno(email).getId();
        return chatInternoRepository.findVisibleForUser(usuarioId).stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public List<MensajeChatResponse> listarMensajesSoporte(UUID chatId, String email, String role) {
        ChatSoporte chat = chatSoporteRepository.findById(chatId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat de soporte no encontrado"));
        autorizarSoporte(chat, email, role);
        return mensajeSoporteRepository.findByIdChatOrderByCreatedAtAsc(chatId)
            .stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public List<MensajeChatResponse> listarMensajesInternos(UUID chatId, String email) {
        ChatInterno chat = chatInternoRepository.findById(chatId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat interno no encontrado"));
        Usuario usuario = usuarioInterno(email);
        autorizarInterno(chat, usuario.getId());
        return chatMensajeInternoRepository.findByIdChatOrderByCreatedAtAsc(chatId)
            .stream().map(this::mapear).toList();
    }

    @Transactional
    public MensajeChatResponse enviarMensajeSoporte(
        UUID chatId,
        String email,
        String role,
        EnviarMensajeChatRequest request
    ) {
        ChatSoporte chat = chatSoporteRepository.findByIdForUpdate(chatId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat de soporte no encontrado"));
        Identity identity = autorizarSoporte(chat, email, role);
        if ("CERRADO".equals(chat.getEstado())) {
            throw new BusinessException("No se puede enviar mensajes a un chat cerrado");
        }

        MensajeSoporte mensaje = new MensajeSoporte();
        mensaje.setIdChat(chatId);
        mensaje.setContenido(contenidoValido(request));
        if (identity.cliente() != null) {
            mensaje.setTipoRemitente("CLIENTE");
            mensaje.setIdCliente(identity.cliente().getId());
        } else {
            mensaje.setTipoRemitente("PERSONAL");
            mensaje.setIdUsuario(identity.usuario().getId());
            if (chat.getIdUsuarioAsignado() == null) {
                chat.setIdUsuarioAsignado(identity.usuario().getId());
                chat.setEstado("EN_ATENCION");
                chatSoporteRepository.save(chat);
            }
        }

        mensajeSoporteRepository.saveAndFlush(mensaje);
        entityManager.refresh(mensaje);
        MensajeChatResponse response = mapear(mensaje);
        eventPublisher.publishEvent(new MensajeChatEvent("soporte", chatId, response));
        return response;
    }

    @Transactional
    public MensajeChatResponse enviarMensajeInterno(
        UUID chatId,
        String email,
        EnviarMensajeChatRequest request
    ) {
        ChatInterno chat = chatInternoRepository.findById(chatId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat interno no encontrado"));
        Usuario usuario = usuarioInterno(email);
        autorizarInterno(chat, usuario.getId());
        if ("CERRADO".equals(chat.getEstado())) {
            throw new BusinessException("No se puede enviar mensajes a un chat cerrado");
        }

        ChatMensajeInterno mensaje = new ChatMensajeInterno();
        mensaje.setIdChat(chatId);
        mensaje.setIdUsuario(usuario.getId());
        mensaje.setContenido(contenidoValido(request));
        chatMensajeInternoRepository.saveAndFlush(mensaje);
        entityManager.refresh(mensaje);
        MensajeChatResponse response = mapear(mensaje);
        eventPublisher.publishEvent(new MensajeChatEvent("interno", chatId, response));
        return response;
    }

    @Transactional(readOnly = true)
    public boolean puedeAcceder(String tipo, UUID chatId, String email, String role) {
        if (tipo.equals("soporte")) {
            ChatSoporte chat = chatSoporteRepository.findById(chatId).orElse(null);
            return chat != null && accesoSoporteValido(chat, email, role);
        }
        if (tipo.equals("interno")) {
            ChatInterno chat = chatInternoRepository.findById(chatId).orElse(null);
            Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(this::esUsuarioInternoActivo).orElse(null);
            return chat != null && usuario != null
                && (usuario.getId().equals(chat.getIdUsuarioCreador())
                    || usuario.getId().equals(chat.getIdUsuarioDestino()));
        }
        return false;
    }

    private Identity autorizarSoporte(ChatSoporte chat, String email, String role) {
        if ("CLIENTE".equals(role)) {
            Cliente cliente = clienteActivo(email);
            if (!cliente.getId().equals(chat.getIdCliente())) {
                throw new ForbiddenException("No tienes acceso a este chat de soporte");
            }
            return new Identity(cliente, null);
        }
        Usuario usuario = usuarioInterno(email);
        boolean asignado = chat.getIdUsuarioAsignado() == null
            || usuario.getId().equals(chat.getIdUsuarioAsignado())
            || usuario.getId().equals(chat.getIdUsuarioCreador());
        if (!asignado) {
            throw new ForbiddenException("Este chat está asignado a otro usuario");
        }
        return new Identity(null, usuario);
    }

    private boolean accesoSoporteValido(ChatSoporte chat, String email, String role) {
        if ("CLIENTE".equals(role)) {
            return clienteRepository.findByEmail(email)
                .filter(this::esClienteActivo)
                .map(cliente -> cliente.getId().equals(chat.getIdCliente()))
                .orElse(false);
        }
        if (!ROLES_INTERNOS.contains(role)) {
            return false;
        }
        return usuarioRepository.findByEmail(email)
            .filter(this::esUsuarioInternoActivo)
            .map(usuario -> chat.getIdUsuarioAsignado() == null
                || usuario.getId().equals(chat.getIdUsuarioAsignado())
                || usuario.getId().equals(chat.getIdUsuarioCreador()))
            .orElse(false);
    }

    private void autorizarInterno(ChatInterno chat, UUID usuarioId) {
        if (!usuarioId.equals(chat.getIdUsuarioCreador()) && !usuarioId.equals(chat.getIdUsuarioDestino())) {
            throw new ForbiddenException("No tienes acceso a este chat interno");
        }
    }

    private Cliente clienteActivo(String email) {
        return clienteRepository.findByEmail(email)
            .filter(this::esClienteActivo)
            .orElseThrow(() -> new ForbiddenException("Se requiere una cuenta de cliente activa"));
    }

    private Usuario usuarioInterno(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .filter(this::esUsuarioInternoActivo)
            .orElseThrow(() -> new ForbiddenException("Se requiere una cuenta interna activa"));
        return usuario;
    }

    private boolean esClienteActivo(Cliente cliente) {
        return Boolean.TRUE.equals(cliente.getActivo());
    }

    private boolean esUsuarioInternoActivo(Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getActivo())
            && usuario.getRol() != null
            && ROLES_INTERNOS.contains(usuario.getRol().getCodigo());
    }

    private String normalizarAsunto(String asunto) {
        return asunto == null || asunto.isBlank() ? null : asunto.trim();
    }

    private String contenidoValido(EnviarMensajeChatRequest request) {
        if (request == null || request.contenido() == null) {
            throw new BusinessException("El mensaje no puede estar vacío");
        }
        String contenido = request.contenido().trim();
        if (contenido.isBlank() || contenido.length() > 4000) {
            throw new BusinessException("El mensaje debe tener entre 1 y 4000 caracteres");
        }
        return contenido;
    }

    private ChatResponse mapear(ChatSoporte chat) {
        return new ChatResponse(
            chat.getId(), chat.getCodigo(), "soporte", chat.getIdCliente(), chat.getIdPedido(),
            chat.getIdUsuarioCreador(), chat.getIdUsuarioAsignado(), null, chat.getAsunto(),
            chat.getEstado(), chat.getCreatedAt(), chat.getUpdatedAt()
        );
    }

    private ChatResponse mapear(ChatInterno chat) {
        return new ChatResponse(
            chat.getId(), chat.getCodigo(), "interno", null, null, chat.getIdUsuarioCreador(),
            null, chat.getIdUsuarioDestino(), chat.getAsunto(), chat.getEstado(),
            chat.getCreatedAt(), chat.getUpdatedAt()
        );
    }

    private MensajeChatResponse mapear(MensajeSoporte mensaje) {
        return new MensajeChatResponse(
            mensaje.getId(), mensaje.getIdChat(),
            mensaje.getIdCliente() == null ? mensaje.getIdUsuario() : mensaje.getIdCliente(),
            mensaje.getTipoRemitente(), mensaje.getContenido(), mensaje.getLeido(), mensaje.getCreatedAt()
        );
    }

    private MensajeChatResponse mapear(ChatMensajeInterno mensaje) {
        return new MensajeChatResponse(
            mensaje.getId(), mensaje.getIdChat(), mensaje.getIdUsuario(), "PERSONAL",
            mensaje.getContenido(), mensaje.getLeido(), mensaje.getCreatedAt()
        );
    }

    private record Identity(Cliente cliente, Usuario usuario) {}
}
