package com.ceramax.api.service.catalogo;

import com.ceramax.api.dto.response.CatalogoPublicoResponse;
import com.ceramax.api.dto.response.GaleriaImagenResponse;
import com.ceramax.api.dto.response.ProductoDetallePublicoResponse;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.catalogo.GaleriaImagenMapper;
import com.ceramax.api.repository.catalogo.ProductoRepository;
import com.ceramax.api.repository.catalogo.CatalogoPublicoProjection;
import com.ceramax.api.repository.catalogo.CatalogoPublicoRepository;
import com.ceramax.api.repository.catalogo.GaleriaImagenRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoPublicoService {

    private final CatalogoPublicoRepository catalogoPublicoRepository;
    private final GaleriaImagenRepository galeriaImagenRepository;
    private final GaleriaImagenMapper galeriaImagenMapper;
    private final ProductoRepository productoRepository;

    public CatalogoPublicoService(
        CatalogoPublicoRepository catalogoPublicoRepository,
        GaleriaImagenRepository galeriaImagenRepository,
        GaleriaImagenMapper galeriaImagenMapper,
        ProductoRepository productoRepository
    ) {
        this.catalogoPublicoRepository = catalogoPublicoRepository;
        this.galeriaImagenRepository = galeriaImagenRepository;
        this.galeriaImagenMapper = galeriaImagenMapper;
        this.productoRepository = productoRepository;
    }

    public List<CatalogoPublicoResponse> listar() {
        return catalogoPublicoRepository.listar().stream().map(this::toResponse).toList();
    }

    public CatalogoPublicoResponse obtener(UUID id) {
        return catalogoPublicoRepository.buscarPorId(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    public ProductoDetallePublicoResponse obtenerDetalle(UUID id) {
        CatalogoPublicoResponse catalogo = obtener(id);
        List<java.util.Map<String, Object>> especificaciones = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"))
            .getEspecificaciones();
        return new ProductoDetallePublicoResponse(
            catalogo.id(),
            catalogo.codigo(),
            catalogo.nombre(),
            catalogo.categoria(),
            catalogo.descripcion(),
            especificaciones == null ? List.of() : especificaciones,
            catalogo.precio(),
            catalogo.precioFinal(),
            catalogo.descuentoPorcentaje(),
            catalogo.stockDisponible()
        );
    }

    public List<GaleriaImagenResponse> listarGaleria(UUID productoId) {
        obtener(productoId);
        return galeriaImagenRepository.findByProducto_IdOrderByOrdenAsc(productoId)
            .stream()
            .map(galeriaImagenMapper::toResponse)
            .toList();
    }

    private CatalogoPublicoResponse toResponse(CatalogoPublicoProjection projection) {
        return new CatalogoPublicoResponse(
            projection.getIdProducto(),
            projection.getProductoCodigo(),
            projection.getProductoNombre(),
            projection.getCategoriaNombre(),
            projection.getDescripcion(),
            projection.getPrecio(),
            projection.getPrecioFinal(),
            projection.getDescuentoPorcentaje(),
            projection.getStockDisponible()
        );
    }
}
