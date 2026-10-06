package com.ceramax.api.service.catalogo;

import com.ceramax.api.dto.request.GaleriaImagenRequest;
import com.ceramax.api.dto.response.GaleriaImagenResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.catalogo.GaleriaImagenMapper;
import com.ceramax.api.model.catalogo.GaleriaImagen;
import com.ceramax.api.model.catalogo.Producto;
import com.ceramax.api.repository.catalogo.GaleriaImagenRepository;
import com.ceramax.api.repository.catalogo.ProductoRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class GaleriaImagenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GaleriaImagenService.class);
    private final GaleriaImagenRepository galeriaImagenRepository;
    private final ProductoRepository productoRepository;
    private final GaleriaImagenMapper galeriaImagenMapper;
    private final EntityManager entityManager;
    private final CloudinaryStorageService cloudinaryStorageService;

    public GaleriaImagenService(
        GaleriaImagenRepository galeriaImagenRepository,
        ProductoRepository productoRepository,
        GaleriaImagenMapper galeriaImagenMapper,
        EntityManager entityManager,
        CloudinaryStorageService cloudinaryStorageService
    ) {
        this.galeriaImagenRepository = galeriaImagenRepository;
        this.productoRepository = productoRepository;
        this.galeriaImagenMapper = galeriaImagenMapper;
        this.entityManager = entityManager;
        this.cloudinaryStorageService = cloudinaryStorageService;
    }

    @Transactional(readOnly = true)
    public List<GaleriaImagenResponse> listarPorProducto(UUID productoId) {
        return galeriaImagenRepository.findByProducto_IdOrderByOrdenAsc(productoId)
            .stream()
            .map(galeriaImagenMapper::toResponse)
            .toList();
    }

    public GaleriaImagenResponse crear(UUID productoId, GaleriaImagenRequest request) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        GaleriaImagen galeriaImagen = galeriaImagenMapper.toEntity(request);
        galeriaImagen.setProducto(producto);
        if (galeriaImagen.getOrden() == null) {
            galeriaImagen.setOrden((short) 0);
        }
        if (galeriaImagen.getEsPrincipal() == null) {
            galeriaImagen.setEsPrincipal(false);
        }
        if (Boolean.TRUE.equals(galeriaImagen.getEsPrincipal())) {
            quitarPrincipalAnterior(productoId, null);
        }
        galeriaImagenRepository.saveAndFlush(galeriaImagen);
        entityManager.refresh(galeriaImagen);
        return galeriaImagenMapper.toResponse(galeriaImagen);
    }

    public GaleriaImagenResponse cargarArchivo(
        UUID productoId,
        MultipartFile archivo,
        Short orden,
        Boolean esPrincipal
    ) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        CloudinaryStorageService.CloudinaryAsset asset = cloudinaryStorageService.upload(
            archivo,
            obtenerCarpeta(producto),
            null
        );
        GaleriaImagen imagen = new GaleriaImagen();
        imagen.setProducto(producto);
        imagen.setUrl(asset.url());
        imagen.setPublicId(asset.publicId());
        imagen.setOrden(orden == null ? siguienteOrden(productoId) : orden);
        imagen.setEsPrincipal(Boolean.TRUE.equals(esPrincipal));
        try {
            if (Boolean.TRUE.equals(imagen.getEsPrincipal())) {
                quitarPrincipalAnterior(productoId, null);
            }
            galeriaImagenRepository.saveAndFlush(imagen);
            entityManager.refresh(imagen);
            return galeriaImagenMapper.toResponse(imagen);
        } catch (RuntimeException exception) {
            eliminarArchivoCompensatorio(asset.publicId(), exception);
            throw exception;
        }
    }

    public GaleriaImagenResponse actualizar(UUID productoId, UUID imagenId, GaleriaImagenRequest request) {
        GaleriaImagen galeriaImagen = galeriaImagenRepository.findById(imagenId)
            .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada"));
        if (!productoId.equals(galeriaImagen.getProducto().getId())) {
            throw new BusinessException("La imagen no pertenece al producto indicado");
        }
        if (Boolean.TRUE.equals(request.esPrincipal())) {
            quitarPrincipalAnterior(productoId, imagenId);
        }
        galeriaImagenMapper.updateEntityFromRequest(request, galeriaImagen);
        galeriaImagenRepository.saveAndFlush(galeriaImagen);
        entityManager.refresh(galeriaImagen);
        return galeriaImagenMapper.toResponse(galeriaImagen);
    }

    public GaleriaImagenResponse reemplazarArchivo(
        UUID productoId,
        UUID imagenId,
        MultipartFile archivo,
        Short orden,
        Boolean esPrincipal
    ) {
        GaleriaImagen imagen = obtenerImagen(productoId, imagenId);
        if (Boolean.TRUE.equals(esPrincipal)) {
            quitarPrincipalAnterior(productoId, imagenId);
        }
        String publicIdAnterior = imagen.getPublicId();
        CloudinaryStorageService.CloudinaryAsset asset = cloudinaryStorageService.upload(
            archivo,
            obtenerCarpeta(imagen.getProducto()),
            publicIdAnterior
        );
        imagen.setUrl(asset.url());
        imagen.setPublicId(asset.publicId());
        if (orden != null) {
            imagen.setOrden(orden);
        }
        if (esPrincipal != null) {
            imagen.setEsPrincipal(esPrincipal);
        }
        galeriaImagenRepository.saveAndFlush(imagen);
        entityManager.refresh(imagen);
        return galeriaImagenMapper.toResponse(imagen);
    }

    public void eliminar(UUID productoId, UUID imagenId) {
        GaleriaImagen galeriaImagen = obtenerImagen(productoId, imagenId);
        cloudinaryStorageService.delete(galeriaImagen.getPublicId());
        galeriaImagenRepository.delete(galeriaImagen);
    }

    private GaleriaImagen obtenerImagen(UUID productoId, UUID imagenId) {
        GaleriaImagen imagen = galeriaImagenRepository.findById(imagenId)
            .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada"));
        if (!productoId.equals(imagen.getProducto().getId())) {
            throw new ResourceNotFoundException("Imagen no encontrada para el producto");
        }
        return imagen;
    }

    private Short siguienteOrden(UUID productoId) {
        return (short) galeriaImagenRepository.findByProducto_IdOrderByOrdenAsc(productoId).size();
    }

    private void quitarPrincipalAnterior(UUID productoId, UUID imagenPrincipalId) {
        List<GaleriaImagen> principalesAnteriores = galeriaImagenRepository
            .findByProducto_IdOrderByOrdenAsc(productoId)
            .stream()
            .filter(imagen -> !imagen.getId().equals(imagenPrincipalId))
            .filter(imagen -> Boolean.TRUE.equals(imagen.getEsPrincipal()))
            .toList();
        principalesAnteriores.forEach(imagen -> imagen.setEsPrincipal(false));
        if (!principalesAnteriores.isEmpty()) {
            galeriaImagenRepository.saveAllAndFlush(principalesAnteriores);
        }
    }

    private String obtenerCarpeta(Producto producto) {
        List<GaleriaImagen> imagenes = galeriaImagenRepository.findByProducto_IdOrderByOrdenAsc(producto.getId());
        if (!imagenes.isEmpty()) {
            String publicId = imagenes.getFirst().getPublicId();
            if (publicId != null && publicId.contains("/")) {
                return publicId.substring(0, publicId.lastIndexOf('/'));
            }
        }
        return cloudinaryStorageService.productFolder(
            producto.getCategoria().getNombre(),
            producto.getNombre()
        );
    }

    private void eliminarArchivoCompensatorio(String publicId, RuntimeException originalException) {
        try {
            cloudinaryStorageService.delete(publicId);
        } catch (RuntimeException cleanupException) {
            LOGGER.error("Could not remove uploaded Cloudinary asset {} after database failure", publicId, cleanupException);
            originalException.addSuppressed(cleanupException);
        }
    }
}
