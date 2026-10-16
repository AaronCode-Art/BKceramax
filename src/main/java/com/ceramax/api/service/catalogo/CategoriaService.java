package com.ceramax.api.service.catalogo;

import com.ceramax.api.dto.request.CategoriaRequest;
import com.ceramax.api.dto.response.CategoriaResponse;
import com.ceramax.api.exception.ConflictException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.mappers.catalogo.CategoriaMapper;
import com.ceramax.api.model.catalogo.Categoria;
import com.ceramax.api.observer.event.CategoriaCreadaEvent;
import com.ceramax.api.repository.catalogo.CategoriaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public CategoriaService(
        CategoriaRepository categoriaRepository,
        CategoriaMapper categoriaMapper,
        EntityManager entityManager,
        ApplicationEventPublisher eventPublisher
    ) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaMapper = categoriaMapper;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    @Cacheable(value = "categorias", key = "'listar'")
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream().map(categoriaMapper::toResponse).toList();
    }

    @Cacheable(value = "categorias", key = "#id")
    @Transactional(readOnly = true)
    public CategoriaResponse obtenerPorId(UUID id) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        return categoriaMapper.toResponse(categoria);
    }

    @CacheEvict(value = "categorias", allEntries = true, beforeInvocation = true)
    public CategoriaResponse crear(CategoriaRequest request) {
        if (categoriaRepository.existsByCodigo(request.codigo()) || categoriaRepository.existsByNombre(request.nombre())) {
            throw new ConflictException("El código o nombre de la categoría ya existe");
        }
        Categoria categoria = categoriaMapper.toEntity(request);
        if (categoria.getActivo() == null) {
            categoria.setActivo(true);
        }
        categoriaRepository.saveAndFlush(categoria);
        entityManager.refresh(categoria);
        eventPublisher.publishEvent(new CategoriaCreadaEvent(categoria.getId(), categoria.getNombre(), null));
        return categoriaMapper.toResponse(categoria);
    }

    @CacheEvict(value = "categorias", allEntries = true, beforeInvocation = true)
    public CategoriaResponse actualizar(UUID id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        if (
            categoriaRepository.existsByCodigoAndIdNot(request.codigo(), id)
                || categoriaRepository.existsByNombreAndIdNot(request.nombre(), id)
        ) {
            throw new ConflictException("El código o nombre de la categoría ya existe");
        }
        categoriaMapper.updateEntityFromRequest(request, categoria);
        categoriaRepository.saveAndFlush(categoria);
        entityManager.refresh(categoria);
        return categoriaMapper.toResponse(categoria);
    }

    @CacheEvict(value = "categorias", allEntries = true, beforeInvocation = true)
    public void desactivar(UUID id) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        categoria.setActivo(false);
        categoriaRepository.saveAndFlush(categoria);
        entityManager.refresh(categoria);
    }
}
