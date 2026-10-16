package com.ceramax.api.service.soporte;

import com.ceramax.api.dto.request.CrearChatInternoRequest;
import com.ceramax.api.dto.request.CrearChatSoporteRequest;
import com.ceramax.api.dto.request.CrearChatSoporteParaClienteRequest;
import com.ceramax.api.dto.request.EnviarMensajeChatRequest;
import com.ceramax.api.dto.response.ChatAdjuntoResponse;
import com.ceramax.api.dto.response.ChatInternoMiembroResponse;
import com.ceramax.api.dto.response.ChatInternoUsuarioResponse;
import com.ceramax.api.dto.response.ChatResponse;
import com.ceramax.api.dto.response.MensajeChatResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ForbiddenException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.soporte.ChatMapper;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.model.soporte.ChatInterno;
import com.ceramax.api.model.soporte.ChatInternoMiembro;
import com.ceramax.api.model.soporte.ChatMensajeInterno;
import com.ceramax.api.model.soporte.ChatMensajeInternoAdjunto;
import com.ceramax.api.model.soporte.ChatSoporte;
import com.ceramax.api.model.soporte.MensajeSoporte;
import com.ceramax.api.observer.event.MensajeChatEvent;
import com.ceramax.api.repository.acceso.ClienteRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import com.ceramax.api.repository.soporte.ChatInternoRepository;
import com.ceramax.api.repository.soporte.ChatInternoMiembroRepository;
import com.ceramax.api.repository.soporte.ChatMensajeInternoAdjuntoRepository;
import com.ceramax.api.repository.soporte.ChatMensajeInternoRepository;
import com.ceramax.api.repository.soporte.ChatSoporteRepository;
import com.ceramax.api.repository.soporte.MensajeSoporteRepository;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.UUID;
import com.ceramax.api.service.catalogo.CloudinaryStorageService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private static final Set<String> ROLES_INTERNOS = Set.of("ADMIN", "JEFE_LOGISTICA", "LOGISTICA", "VENDEDOR", "DELIVERY");
    private static final Set<String> ROLES_CREAR_CHAT = Set.of("ADMIN", "JEFE_LOGISTICA", "LOGISTICA");

    private final ChatSoporteRepository chatSoporteRepository;
    private final MensajeSoporteRepository mensajeSoporteRepository;
    private final ChatInternoRepository chatInternoRepository;
    private final ChatInternoMiembroRepository chatInternoMiembroRepository;
    private final ChatMensajeInternoRepository chatMensajeInternoRepository;
    private final ChatMensajeInternoAdjuntoRepository chatMensajeInternoAdjuntoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final ChatMapper chatMapper;

    public ChatService(
        ChatSoporteRepository chatSoporteRepository,
        MensajeSoporteRepository mensajeSoporteRepository,
        ChatInternoRepository chatInternoRepository,
        ChatInternoMiembroRepository chatInternoMiembroRepository,
        ChatMensajeInternoRepository chatMensajeInternoRepository,
        ChatMensajeInternoAdjuntoRepository chatMensajeInternoAdjuntoRepository,
        ClienteRepository clienteRepository,
        UsuarioRepository usuarioRepository,
        ApplicationEventPublisher eventPublisher,
        EntityManager entityManager,
        CloudinaryStorageService cloudinaryStorageService,
        ChatMapper chatMapper
    ) {
        this.chatSoporteRepository = chatSoporteRepository;
        this.mensajeSoporteRepository = mensajeSoporteRepository;
        this.chatInternoRepository = chatInternoRepository;
        this.chatInternoMiembroRepository = chatInternoMiembroRepository;
        this.chatMensajeInternoRepository = chatMensajeInternoRepository;
        this.chatMensajeInternoAdjuntoRepository = chatMensajeInternoAdjuntoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.cloudinaryStorageService = cloudinaryStorageService;
        this.chatMapper = chatMapper;
    }

    @Transactional
    public ChatResponse crearChatSoporte(String email, CrearChatSoporteRequest request) {
        Cliente cliente = clienteActivo(email);
        ChatSoporte chat = new ChatSoporte();
        chat.setIdCliente(cliente.getId());
        chat.setAsunto(normalizarAsunto(request.asunto()));
        chatSoporteRepository.saveAndFlush(chat);
        entityManager.refresh(chat);
        return chatMapper.toSupportChat(chat);
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
        return chatMapper.toSupportChat(chat);
    }

    @Transactional
    public ChatResponse crearChatInterno(String email, CrearChatInternoRequest request) {
        Usuario creador = usuarioInterno(email);
        if (!ROLES_CREAR_CHAT.contains(creador.getRol().getCodigo())) {
            throw new ForbiddenException("Solo ADMIN, JEFE_LOGISTICA o LOGISTICA pueden crear chats internos");
        }
        Set<UUID> miembroIds = new LinkedHashSet<>(
            request.miembroIds() == null ? List.of() : request.miembroIds()
        );
        if (request.usuarioDestinoId() != null) {
            miembroIds.add(request.usuarioDestinoId());
        }
        miembroIds.remove(creador.getId());
        if (miembroIds.isEmpty()) {
            throw new BusinessException("Selecciona al menos un miembro para la conversación");
        }
        List<Usuario> miembros = usuarioRepository.findAllById(miembroIds).stream()
            .filter(this::esUsuarioInternoActivo)
            .toList();
        if (miembros.size() != miembroIds.size()) {
            throw new ResourceNotFoundException("Uno o más miembros no son usuarios internos activos");
        }

        ChatInterno chat = new ChatInterno();
        chat.setIdUsuarioCreador(creador.getId());
        chat.setAsunto(normalizarAsunto(request.asunto()));
        chat.setIdUsuarioDestino(miembros.size() == 1 ? miembros.getFirst().getId() : null);
        chatInternoRepository.saveAndFlush(chat);
        entityManager.refresh(chat);
        List<ChatInternoMiembro> registros = new java.util.ArrayList<>();
        registros.add(crearRegistroMiembro(chat.getId(), creador.getId()));
        miembros.stream()
            .map(miembro -> crearRegistroMiembro(chat.getId(), miembro.getId()))
            .forEach(registros::add);
        chatInternoMiembroRepository.saveAllAndFlush(registros);
        return chatMapper.toInternalChat(chat, chatInternoMiembroRepository.listarMiembros(chat.getId()));
    }

    @Transactional(readOnly = true)
    public List<ChatInternoUsuarioResponse> listarUsuariosChatInterno(String email) {
        Usuario usuarioActual = usuarioInterno(email);
        return usuarioRepository.findByActivoTrueAndRol_CodigoInOrderByApellidosAscNombresAsc(ROLES_INTERNOS)
            .stream()
            .filter(usuario -> !usuario.getId().equals(usuarioActual.getId()))
            .filter(this::esUsuarioInternoActivo)
            .map(usuario -> new ChatInternoUsuarioResponse(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getEmail(),
                usuario.getRol().getCodigo()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<ChatResponse> listarChatsSoporte(String email, String role, Pageable pageable) {
        if ("CLIENTE".equals(role)) {
            UUID clienteId = clienteActivo(email).getId();
            return chatSoporteRepository.findByIdClienteOrderByUpdatedAtDescIdDesc(clienteId, pageable)
                .map(chatMapper::toSupportChat);
        }
        Usuario usuario = usuarioInterno(email);
        return chatSoporteRepository.findVisibleForStaff(usuario.getId(), pageable).map(chatMapper::toSupportChat);
    }

    @Transactional(readOnly = true)
    public Page<ChatResponse> listarChatsInternos(String email, Pageable pageable) {
        Usuario usuario = usuarioInterno(email);
        Page<ChatInterno> chats = chatInternoRepository.findVisibleToMember(usuario.getId(), pageable);
        Map<UUID, List<ChatInternoMiembroResponse>> miembros = listarMiembros(
            chats.getContent().stream().map(ChatInterno::getId).toList()
        );
        return chats.map(chat -> chatMapper.toInternalChat(chat, miembros.getOrDefault(chat.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public Page<MensajeChatResponse> listarMensajesSoporte(
        UUID chatId,
        String email,
        String role,
        Pageable pageable
    ) {
        ChatSoporte chat = chatSoporteRepository.findById(chatId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat de soporte no encontrado"));
        autorizarSoporte(chat, email, role);
        return mensajeSoporteRepository.findByIdChatOrderByCreatedAtAscIdAsc(chatId, pageable)
            .map(chatMapper::toSupportMessage);
    }

    @Transactional(readOnly = true)
    public Page<MensajeChatResponse> listarMensajesInternos(UUID chatId, String email, Pageable pageable) {
        ChatInterno chat = chatInternoRepository.findById(chatId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat interno no encontrado"));
        exigirMiembro(chatId, usuarioInterno(email));
        Page<ChatMensajeInterno> mensajes = chatMensajeInternoRepository
            .findByIdChatOrderByCreatedAtAscIdAsc(chatId, pageable);
        List<UUID> idsMensaje = mensajes.getContent().stream().map(ChatMensajeInterno::getId).toList();
        Map<UUID, List<ChatAdjuntoResponse>> adjuntos = adjuntosPorMensaje(idsMensaje);
        return mensajes.map(mensaje -> chatMapper.toInternalMessage(
            mensaje,
            adjuntos.getOrDefault(mensaje.getId(), List.of())
        ));
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
        MensajeChatResponse response = chatMapper.toSupportMessage(mensaje);
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
        exigirMiembro(chatId, usuario);
        if ("CERRADO".equals(chat.getEstado())) {
            throw new BusinessException("No se puede enviar mensajes a un chat cerrado");
        }

        ChatMensajeInterno mensaje = new ChatMensajeInterno();
        mensaje.setIdChat(chatId);
        mensaje.setIdUsuario(usuario.getId());
        mensaje.setContenido(contenidoValido(request));
        chatMensajeInternoRepository.saveAndFlush(mensaje);
        entityManager.refresh(mensaje);
        MensajeChatResponse response = chatMapper.toInternalMessage(mensaje, List.of());
        eventPublisher.publishEvent(new MensajeChatEvent("interno", chatId, response));
        return response;
    }

    @Transactional
    public MensajeChatResponse enviarAdjuntoInterno(
        UUID chatId,
        String email,
        String contenido,
        MultipartFile archivo
    ) {
        ChatInterno chat = chatInternoRepository.findById(chatId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat interno no encontrado"));
        Usuario usuario = usuarioInterno(email);
        exigirMiembro(chatId, usuario);
        if ("CERRADO".equals(chat.getEstado())) {
            throw new BusinessException("No se puede enviar archivos a un chat cerrado");
        }

        String texto = contenido == null ? "" : contenido.trim();
        if (texto.length() > 4000) {
            throw new BusinessException("El mensaje no puede superar los 4000 caracteres");
        }
        CloudinaryStorageService.ChatCloudinaryAsset asset =
            cloudinaryStorageService.uploadChatAttachment(archivo, chatId);
        try {
            ChatMensajeInterno mensaje = new ChatMensajeInterno();
            mensaje.setIdChat(chatId);
            mensaje.setIdUsuario(usuario.getId());
            mensaje.setContenido(texto);
            chatMensajeInternoRepository.saveAndFlush(mensaje);
            entityManager.refresh(mensaje);

            ChatMensajeInternoAdjunto adjunto = new ChatMensajeInternoAdjunto();
            adjunto.setIdMensaje(mensaje.getId());
            adjunto.setNombreArchivo(nombreArchivo(archivo.getOriginalFilename()));
            adjunto.setUrl(asset.url());
            adjunto.setPublicId(asset.publicId());
            adjunto.setTipoContenido(archivo.getContentType());
            adjunto.setTamanoBytes(archivo.getSize());
            chatMensajeInternoAdjuntoRepository.saveAndFlush(adjunto);
            entityManager.refresh(adjunto);

            MensajeChatResponse response = chatMapper.toInternalMessage(
                mensaje,
                List.of(chatMapper.toAttachment(adjunto))
            );
            eventPublisher.publishEvent(new MensajeChatEvent("interno", chatId, response));
            return response;
        } catch (RuntimeException exception) {
            try {
                cloudinaryStorageService.deleteChatAttachment(asset.publicId(), asset.resourceType());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional
    public ChatResponse actualizarNombreChatInterno(UUID chatId, String email, String asunto) {
        ChatInterno chat = chatInternoRepository.findById(chatId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat interno no encontrado"));
        Usuario usuario = usuarioInterno(email);
        exigirMiembro(chatId, usuario);
        autorizarAdministracionChat(chat, usuario);
        if ("CERRADO".equals(chat.getEstado())) {
            throw new BusinessException("No se puede cambiar el nombre de un chat cerrado");
        }
        chat.setAsunto(normalizarAsunto(asunto));
        ChatInterno guardado = chatInternoRepository.saveAndFlush(chat);
        return chatMapper.toInternalChat(guardado, chatInternoMiembroRepository.listarMiembros(chatId));
    }

    @Transactional
    public void cerrarChatInterno(UUID chatId, String email) {
        ChatInterno chat = chatInternoRepository.findById(chatId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat interno no encontrado"));
        Usuario usuario = usuarioInterno(email);
        exigirMiembro(chatId, usuario);
        autorizarAdministracionChat(chat, usuario);
        chat.setEstado("CERRADO");
        chatInternoRepository.save(chat);
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
            return chat != null
                && usuario != null
                && ROLES_INTERNOS.contains(role)
                && chatInternoMiembroRepository.existsByIdChatAndIdUsuario(chatId, usuario.getId());
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

    private void autorizarAdministracionChat(ChatInterno chat, Usuario usuario) {
        boolean creador = usuario.getId().equals(chat.getIdUsuarioCreador());
        boolean administrador = "ADMIN".equals(usuario.getRol().getCodigo());
        if (!creador && !administrador) {
            throw new ForbiddenException("Solo quien creó el chat o ADMIN puede cambiarlo o cerrarlo");
        }
    }

    private void exigirMiembro(UUID chatId, Usuario usuario) {
        if (!chatInternoMiembroRepository.existsByIdChatAndIdUsuario(chatId, usuario.getId())) {
            throw new ForbiddenException("No eres miembro de esta conversación");
        }
    }

    private ChatInternoMiembro crearRegistroMiembro(UUID chatId, UUID usuarioId) {
        ChatInternoMiembro miembro = new ChatInternoMiembro();
        miembro.setIdChat(chatId);
        miembro.setIdUsuario(usuarioId);
        return miembro;
    }

    private Map<UUID, List<ChatInternoMiembroResponse>> listarMiembros(Collection<UUID> chatIds) {
        if (chatIds.isEmpty()) {
            return Map.of();
        }
        return chatInternoMiembroRepository.listarMiembrosPorChats(chatIds).stream()
            .collect(Collectors.groupingBy(ChatInternoMiembroResponse::chatId));
    }

    private Map<UUID, List<ChatAdjuntoResponse>> adjuntosPorMensaje(Collection<UUID> idsMensaje) {
        if (idsMensaje.isEmpty()) {
            return Map.of();
        }
        return chatMensajeInternoAdjuntoRepository.findByIdMensajeInOrderByCreatedAtAsc(idsMensaje).stream()
            .collect(Collectors.groupingBy(
                ChatMensajeInternoAdjunto::getIdMensaje,
                Collectors.mapping(chatMapper::toAttachment, Collectors.toList())
            ));
    }

    private String nombreArchivo(String nombreOriginal) {
        String nombre = nombreOriginal == null ? "" : nombreOriginal.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        if (nombre.isBlank()) {
            throw new BusinessException("El archivo debe tener un nombre válido");
        }
        return nombre.length() <= 255 ? nombre : nombre.substring(nombre.length() - 255);
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

    private record Identity(Cliente cliente, Usuario usuario) {}
}
