package com.ceramax.api.service.catalogo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.request.CategoriaRequest;
import com.ceramax.api.dto.response.CategoriaResponse;
import com.ceramax.api.mappers.catalogo.CategoriaMapper;
import com.ceramax.api.model.catalogo.Categoria;
import com.ceramax.api.repository.catalogo.CategoriaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class CategoriaCacheTest {

    @Test
    void reutilizaListadoYLoInvalidaAlCrearCategoria() {
        CategoriaRepository repository = mock(CategoriaRepository.class);
        CategoriaMapper mapper = mock(CategoriaMapper.class);
        EntityManager entityManager = mock(EntityManager.class);
        Categoria existente = new Categoria();
        Categoria nueva = new Categoria();
        CategoriaRequest request = new CategoriaRequest("DEC", "Decoración", null, true);
        CategoriaResponse response = new CategoriaResponse(
            UUID.randomUUID(),
            "DEC",
            "Decoración",
            null,
            true,
            null,
            null
        );
        when(repository.findAll()).thenReturn(List.of(existente));
        when(repository.existsByCodigo("DEC")).thenReturn(false);
        when(repository.existsByNombre("Decoración")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(nueva);
        when(mapper.toResponse(any(Categoria.class))).thenReturn(response);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(CategoriaRepository.class, () -> repository);
            context.registerBean(CategoriaMapper.class, () -> mapper);
            context.registerBean(EntityManager.class, () -> entityManager);
            context.register(CacheTestConfiguration.class);
            context.refresh();

            CategoriaService service = context.getBean(CategoriaService.class);
            service.listar();
            service.listar();
            assertNotNull(context.getBean(CacheManager.class).getCache("categorias").get("listar"));
            verify(repository, times(1)).findAll();

            service.crear(request);

            service.listar();
            verify(repository, times(2)).findAll();
        }
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("categorias");
        }

        @Bean
        CategoriaService categoriaService(
            CategoriaRepository repository,
            CategoriaMapper mapper,
            EntityManager entityManager
        ) {
            return new CategoriaService(repository, mapper, entityManager, event -> {});
        }
    }
}
