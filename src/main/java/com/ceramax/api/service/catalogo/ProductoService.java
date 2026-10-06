package com.ceramax.api.service.catalogo;

import com.ceramax.api.dto.request.ProductoRequest;
import com.ceramax.api.dto.response.ProductoResponse;
import com.ceramax.api.exception.ConflictException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.catalogo.ProductoMapper;
import com.ceramax.api.model.catalogo.Categoria;
import com.ceramax.api.model.catalogo.Producto;
import com.ceramax.api.repository.catalogo.CategoriaRepository;
import com.ceramax.api.repository.catalogo.ProductoRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoMapper productoMapper;
    private final EntityManager entityManager;

    public ProductoService(
        ProductoRepository productoRepository,
        CategoriaRepository categoriaRepository,
        ProductoMapper productoMapper,
        EntityManager entityManager
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoMapper = productoMapper;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream().map(productoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(UUID id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        return productoMapper.toResponse(producto);
    }

    public ProductoResponse crear(ProductoRequest request) {
        if (productoRepository.existsByCodigo(request.codigo())) {
            throw new ConflictException("El código del producto ya existe");
        }
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        Producto producto = productoMapper.toEntity(request);
        producto.setCategoria(categoria);
        if (producto.getDescuentoPorcentaje() == null) {
            producto.setDescuentoPorcentaje(BigDecimal.ZERO);
        }
        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }
        productoRepository.saveAndFlush(producto);
        entityManager.refresh(producto);
        return productoMapper.toResponse(producto);
    }

    public ProductoResponse actualizar(UUID id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        if (productoRepository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ConflictException("El código del producto ya existe");
        }

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        productoMapper.updateEntityFromRequest(request, producto);
        producto.setCategoria(categoria);
        productoRepository.saveAndFlush(producto);
        entityManager.refresh(producto);
        return productoMapper.toResponse(producto);
    }

    public void desactivar(UUID id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        producto.setActivo(false);
        productoRepository.saveAndFlush(producto);
        entityManager.refresh(producto);
    }
}
