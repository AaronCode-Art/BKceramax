package com.ceramax.api.service.catalogo;

import com.ceramax.api.dto.response.CatalogoPublicoResponse;
import com.ceramax.api.dto.response.GaleriaImagenResponse;
import com.ceramax.api.dto.response.ProductoDetallePublicoResponse;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.catalogo.CatalogoPublicoMapper;
import com.ceramax.api.mappers.catalogo.GaleriaImagenMapper;
import com.ceramax.api.repository.catalogo.CatalogoPublicoProjection;
import com.ceramax.api.repository.catalogo.CatalogoPublicoRepository;
import com.ceramax.api.repository.catalogo.GaleriaImagenRepository;
import com.ceramax.api.repository.catalogo.ProductoRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoPublicoService {

    private final CatalogoPublicoRepository catalogoPublicoRepository;
    private final GaleriaImagenRepository galeriaImagenRepository;
    private final GaleriaImagenMapper galeriaImagenMapper;
    private final ProductoRepository productoRepository;
    private final CatalogoPublicoMapper catalogoPublicoMapper;

    public CatalogoPublicoService(
        CatalogoPublicoRepository catalogoPublicoRepository,
        GaleriaImagenRepository galeriaImagenRepository,
        GaleriaImagenMapper galeriaImagenMapper,
        ProductoRepository productoRepository,
        CatalogoPublicoMapper catalogoPublicoMapper
    ) {
        this.catalogoPublicoRepository = catalogoPublicoRepository;
        this.galeriaImagenRepository = galeriaImagenRepository;
        this.galeriaImagenMapper = galeriaImagenMapper;
        this.productoRepository = productoRepository;
        this.catalogoPublicoMapper = catalogoPublicoMapper;
    }

    public Page<CatalogoPublicoResponse> listar(Pageable pageable) {
        Page<CatalogoPublicoProjection> productos = catalogoPublicoRepository.listar(pageable);
        List<UUID> productoIds = productos.getContent().stream()
            .map(CatalogoPublicoProjection::getIdProducto)
            .toList();
        Map<UUID, String> imagenesPrincipales = productoIds.isEmpty()
            ? Map.of()
            : galeriaImagenRepository.listarImagenPrincipalPorProductos(productoIds).stream()
                .collect(Collectors.toMap(
                    imagen -> imagen.getProductoId(),
                    imagen -> imagen.getUrl(),
                    (primera, ignorada) -> primera
                ));
        return productos.map(producto -> catalogoPublicoMapper.toResponse(
            producto,
            imagenesPrincipales.get(producto.getIdProducto())
        ));
    }

    public CatalogoPublicoResponse obtener(UUID id) {
        return catalogoPublicoRepository.buscarPorId(id)
            .map(catalogoPublicoMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    public ProductoDetallePublicoResponse obtenerDetalle(UUID id) {
        CatalogoPublicoResponse catalogo = obtener(id);
        List<java.util.Map<String, Object>> especificaciones = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"))
            .getEspecificaciones();
        return catalogoPublicoMapper.toDetail(catalogo, especificaciones);
    }

    @Cacheable(value = "catalogo-galeria", key = "#productoId")
    public List<GaleriaImagenResponse> listarGaleria(UUID productoId) {
        obtener(productoId);
        return galeriaImagenRepository.findByProducto_IdOrderByOrdenAsc(productoId)
            .stream()
            .map(galeriaImagenMapper::toResponse)
            .toList();
    }
}
