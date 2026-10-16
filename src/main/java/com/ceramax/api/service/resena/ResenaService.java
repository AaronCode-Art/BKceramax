package com.ceramax.api.service.resena;

import com.ceramax.api.dto.request.ActualizarResenaRequest;
import com.ceramax.api.dto.request.CrearResenaRequest;
import com.ceramax.api.dto.request.ResenaImagenRequest;
import com.ceramax.api.dto.response.ResenaImagenResponse;
import com.ceramax.api.dto.response.ResenaResponse;
import com.ceramax.api.dto.response.ResumenResenasResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ConflictException;
import com.ceramax.api.exception.ForbiddenException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.resena.ResenaMapper;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.catalogo.Producto;
import com.ceramax.api.model.resena.Resena;
import com.ceramax.api.model.resena.ResenaImagen;
import com.ceramax.api.repository.acceso.ClienteRepository;
import com.ceramax.api.repository.catalogo.ProductoRepository;
import com.ceramax.api.repository.resena.ResenaImagenRepository;
import com.ceramax.api.repository.resena.ResenaPublicaProjection;
import com.ceramax.api.repository.resena.ResenaRepository;
import com.ceramax.api.repository.resena.ResumenResenasProjection;
import com.ceramax.api.repository.venta.PedidoRepository;
import com.ceramax.api.service.catalogo.CloudinaryStorageService;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResenaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResenaService.class);
    private static final int MAX_IMAGES_PER_REVIEW = 5;

    private final ResenaRepository resenaRepository;
    private final ResenaImagenRepository imagenRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final EntityManager entityManager;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final ResenaMapper resenaMapper;

    public ResenaService(
        ResenaRepository resenaRepository,
        ResenaImagenRepository imagenRepository,
        PedidoRepository pedidoRepository,
        ClienteRepository clienteRepository,
        ProductoRepository productoRepository,
        EntityManager entityManager,
        CloudinaryStorageService cloudinaryStorageService,
        ResenaMapper resenaMapper
    ) {
        this.resenaRepository = resenaRepository;
        this.imagenRepository = imagenRepository;
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.entityManager = entityManager;
        this.cloudinaryStorageService = cloudinaryStorageService;
        this.resenaMapper = resenaMapper;
    }

    @Transactional
    public ResenaResponse crear(CrearResenaRequest request) {
        Cliente cliente = clienteAutenticado();
        Producto producto = productoRepository.findById(request.productoId())
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        validarCompraCompletada(request.pedidoId(), cliente.getId(), producto.getId());
        validarImagenes(request.imagenes());

        Resena resena = resenaRepository.findByIdClienteAndIdProducto(cliente.getId(), producto.getId())
            .orElseGet(Resena::new);
        if (resena.getId() != null && Boolean.TRUE.equals(resena.getActivo())) {
            throw new ConflictException("Ya existe una reseña activa para este producto");
        }
        resena.setIdCliente(cliente.getId());
        resena.setIdProducto(producto.getId());
        resena.setIdPedido(request.pedidoId());
        resena.setCalificacion(request.calificacion());
        resena.setComentario(normalizarComentario(request.comentario()));
        resena.setActivo(true);

        resenaRepository.saveAndFlush(resena);
        entityManager.refresh(resena);
        reemplazarImagenes(resena.getId(), request.imagenes());
        return resenaMapper.toResponse(resena, producto, cliente, imagenesPorResena(resena.getId()));
    }

    @Transactional
    public ResenaResponse crearConImagenes(CrearResenaRequest request, List<MultipartFile> archivos) {
        validarCompraParaCarga(request.pedidoId(), request.productoId());
        List<ResenaImagenRequest> imagenesCargadas = cargarImagenes(archivos);
        List<ResenaImagenRequest> imagenes = combinarImagenes(request.imagenes(), imagenesCargadas);
        try {
            return crear(new CrearResenaRequest(
                request.productoId(),
                request.pedidoId(),
                request.calificacion(),
                request.comentario(),
                imagenes
            ));
        } catch (RuntimeException exception) {
            limpiarImagenesCargadas(imagenesCargadas, exception);
            throw exception;
        }
    }

    @Transactional
    public ResenaResponse actualizar(UUID resenaId, ActualizarResenaRequest request) {
        Cliente cliente = clienteAutenticado();
        Resena resena = resenaRepository.findByIdAndIdClienteAndActivoTrue(resenaId, cliente.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada"));
        validarCompraCompletada(request.pedidoId(), cliente.getId(), resena.getIdProducto());
        validarImagenes(request.imagenes());

        resena.setIdPedido(request.pedidoId());
        resena.setCalificacion(request.calificacion());
        resena.setComentario(normalizarComentario(request.comentario()));
        resenaRepository.saveAndFlush(resena);
        entityManager.refresh(resena);
        reemplazarImagenes(resena.getId(), request.imagenes());

        Producto producto = productoRepository.findById(resena.getIdProducto())
            .orElseThrow(() -> new ResourceNotFoundException("Producto de la reseña no encontrado"));
        return resenaMapper.toResponse(resena, producto, cliente, imagenesPorResena(resena.getId()));
    }

    @Transactional
    public ResenaResponse actualizarConImagenes(
        UUID resenaId,
        ActualizarResenaRequest request,
        List<MultipartFile> archivos
    ) {
        validarCompraParaCarga(request.pedidoId(), obtenerProductoResena(resenaId));
        List<ResenaImagenRequest> imagenesCargadas = cargarImagenes(archivos);
        List<ResenaImagenRequest> imagenes = combinarImagenes(request.imagenes(), imagenesCargadas);
        try {
            return actualizar(resenaId, new ActualizarResenaRequest(
                request.pedidoId(),
                request.calificacion(),
                request.comentario(),
                imagenes
            ));
        } catch (RuntimeException exception) {
            limpiarImagenesCargadas(imagenesCargadas, exception);
            throw exception;
        }
    }

    @Transactional
    public void eliminar(UUID resenaId) {
        Cliente cliente = clienteAutenticado();
        Resena resena = resenaRepository.findByIdAndIdClienteAndActivoTrue(resenaId, cliente.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada"));
        resena.setActivo(false);
        resenaRepository.saveAndFlush(resena);
    }

    @Transactional(readOnly = true)
    public Page<ResenaResponse> listarPublicas(UUID productoId, Pageable pageable) {
        validarProducto(productoId);
        Page<ResenaPublicaProjection> resenas = resenaRepository.listarPublicas(productoId, pageable);
        List<UUID> ids = resenas.getContent().stream().map(ResenaPublicaProjection::getId).toList();
        Map<UUID, List<ResenaImagenResponse>> imagenesPorId = agruparImagenes(ids);
        return resenas.map(resena ->
            resenaMapper.toPublicResponse(resena, imagenesPorId.getOrDefault(resena.getId(), List.of()))
        );
    }

    @Transactional(readOnly = true)
    public ResumenResenasResponse obtenerResumen(UUID productoId) {
        validarProducto(productoId);
        ResumenResenasProjection resumen = resenaRepository.obtenerResumen(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Resumen de reseñas no encontrado"));
        return resenaMapper.toSummary(resumen);
    }

    private void validarCompraCompletada(UUID pedidoId, UUID clienteId, UUID productoId) {
        if (!pedidoRepository.existeCompraCompletadaConProducto(pedidoId, clienteId, productoId)) {
            throw new BusinessException(
                "Solo puedes reseñar productos incluidos en uno de tus pedidos completados"
            );
        }
    }

    private void validarCompraParaCarga(UUID pedidoId, UUID productoId) {
        Cliente cliente = clienteAutenticado();
        validarProducto(productoId);
        validarCompraCompletada(pedidoId, cliente.getId(), productoId);
    }

    private UUID obtenerProductoResena(UUID resenaId) {
        Cliente cliente = clienteAutenticado();
        return resenaRepository.findByIdAndIdClienteAndActivoTrue(resenaId, cliente.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada"))
            .getIdProducto();
    }

    private List<ResenaImagenRequest> cargarImagenes(List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            return List.of();
        }
        if (archivos.size() > MAX_IMAGES_PER_REVIEW) {
            throw new BusinessException("Puedes adjuntar como máximo 5 imágenes por reseña.");
        }
        List<ResenaImagenRequest> imagenes = new ArrayList<>(archivos.size());
        try {
            for (MultipartFile archivo : archivos) {
                CloudinaryStorageService.CloudinaryAsset asset = cloudinaryStorageService.upload(
                    archivo,
                    cloudinaryStorageService.reviewImageFolder(),
                    null
                );
                imagenes.add(new ResenaImagenRequest(asset.url(), asset.publicId()));
            }
            return imagenes;
        } catch (RuntimeException exception) {
            limpiarImagenesCargadas(imagenes, exception);
            throw exception;
        }
    }

    private List<ResenaImagenRequest> combinarImagenes(
        List<ResenaImagenRequest> existentes,
        List<ResenaImagenRequest> cargadas
    ) {
        List<ResenaImagenRequest> imagenes = new ArrayList<>();
        if (existentes != null) {
            imagenes.addAll(existentes);
        }
        imagenes.addAll(cargadas);
        if (imagenes.size() > MAX_IMAGES_PER_REVIEW) {
            BusinessException exception = new BusinessException("Puedes adjuntar como máximo 5 imágenes por reseña.");
            limpiarImagenesCargadas(cargadas, exception);
            throw exception;
        }
        return imagenes;
    }

    private void limpiarImagenesCargadas(List<ResenaImagenRequest> imagenes, RuntimeException causa) {
        for (ResenaImagenRequest imagen : imagenes) {
            if (imagen.publicId() == null || imagen.publicId().isBlank()) {
                continue;
            }
            try {
                cloudinaryStorageService.delete(imagen.publicId());
            } catch (RuntimeException cleanupException) {
                LOGGER.error(
                    "Could not remove uploaded review image {} after review operation failed",
                    imagen.publicId(),
                    cleanupException
                );
                causa.addSuppressed(cleanupException);
            }
        }
    }

    private void validarProducto(UUID productoId) {
        if (!productoRepository.existsById(productoId)) {
            throw new ResourceNotFoundException("Producto no encontrado");
        }
    }

    private Cliente clienteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Se requiere autenticación de cliente");
        }
        return clienteRepository.findByEmail(authentication.getName())
            .filter(cliente -> Boolean.TRUE.equals(cliente.getActivo()))
            .orElseThrow(() -> new ForbiddenException("Se requiere una cuenta de cliente activa"));
    }

    private String normalizarComentario(String comentario) {
        return comentario == null || comentario.isBlank() ? null : comentario.trim();
    }

    private void validarImagenes(List<ResenaImagenRequest> imagenes) {
        if (imagenes == null) {
            return;
        }
        for (ResenaImagenRequest imagen : imagenes) {
            try {
                URI uri = new URI(imagen.url().trim());
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                    throw new BusinessException("Las imágenes de reseña deben usar una URL HTTPS válida");
                }
            } catch (URISyntaxException exception) {
                throw new BusinessException("La URL de una imagen de reseña no es válida");
            }
        }
    }

    private void reemplazarImagenes(UUID resenaId, List<ResenaImagenRequest> imagenes) {
        imagenRepository.deleteByIdResena(resenaId);
        if (imagenes == null || imagenes.isEmpty()) {
            return;
        }
        List<ResenaImagen> entidades = new ArrayList<>(imagenes.size());
        for (int indice = 0; indice < imagenes.size(); indice++) {
            if (indice > Short.MAX_VALUE) {
                throw new BusinessException("La reseña excede el máximo de imágenes permitido por el esquema");
            }
            ResenaImagenRequest request = imagenes.get(indice);
            ResenaImagen imagen = new ResenaImagen();
            imagen.setIdResena(resenaId);
            imagen.setUrl(request.url().trim());
            imagen.setPublicId(normalizarPublicId(request.publicId()));
            imagen.setOrden((short) indice);
            entidades.add(imagen);
        }
        imagenRepository.saveAllAndFlush(entidades);
    }

    private String normalizarPublicId(String publicId) {
        return publicId == null || publicId.isBlank() ? null : publicId.trim();
    }

    private List<ResenaImagenResponse> imagenesPorResena(UUID resenaId) {
        return imagenRepository.findByIdResenaOrderByOrdenAsc(resenaId).stream()
            .map(resenaMapper::toImageResponse)
            .toList();
    }

    private Map<UUID, List<ResenaImagenResponse>> agruparImagenes(List<UUID> resenaIds) {
        if (resenaIds.isEmpty()) {
            return Map.of();
        }
        return imagenRepository.findByIdResenaInOrderByIdResenaAscOrdenAsc(resenaIds).stream()
            .map(imagen -> Map.entry(imagen.getIdResena(), resenaMapper.toImageResponse(imagen)))
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                HashMap::new,
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())
            ));
    }

}
