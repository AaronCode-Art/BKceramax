package com.ceramax.api.service.catalogo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ceramax.api.dto.request.CategoriaRequest;
import com.ceramax.api.dto.request.GaleriaImagenRequest;
import com.ceramax.api.dto.request.ProductoRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.multipart.MultipartFile;

class CacheInvalidationPolicyTest {

    @Test
    void invalidaCategoriasAntesDeCadaEscritura() throws NoSuchMethodException {
        assertAllEntriesEvicted(CategoriaService.class, "crear", CategoriaRequest.class, "categorias");
        assertAllEntriesEvicted(
            CategoriaService.class,
            "actualizar",
            new Class<?>[] {java.util.UUID.class, CategoriaRequest.class},
            "categorias"
        );
        assertAllEntriesEvicted(CategoriaService.class, "desactivar", java.util.UUID.class, "categorias");
    }

    @Test
    void invalidaGaleriasAntesDeCadaEscrituraRelacionada() throws NoSuchMethodException {
        assertAllEntriesEvicted(ProductoService.class, "crear", ProductoRequest.class, "catalogo-galeria");
        assertAllEntriesEvicted(
            ProductoService.class,
            "actualizar",
            new Class<?>[] {java.util.UUID.class, ProductoRequest.class},
            "catalogo-galeria"
        );
        assertAllEntriesEvicted(ProductoService.class, "desactivar", java.util.UUID.class, "catalogo-galeria");
        assertProductGalleryEvicted(
            GaleriaImagenService.class,
            "crear",
            java.util.UUID.class,
            GaleriaImagenRequest.class
        );
        assertProductGalleryEvicted(
            GaleriaImagenService.class,
            "cargarArchivo",
            java.util.UUID.class,
            MultipartFile.class,
            Short.class,
            Boolean.class
        );
        assertProductGalleryEvicted(
            GaleriaImagenService.class,
            "actualizar",
            java.util.UUID.class,
            java.util.UUID.class,
            GaleriaImagenRequest.class
        );
        assertProductGalleryEvicted(
            GaleriaImagenService.class,
            "reemplazarArchivo",
            java.util.UUID.class,
            java.util.UUID.class,
            MultipartFile.class,
            Short.class,
            Boolean.class
        );
        assertProductGalleryEvicted(
            GaleriaImagenService.class,
            "eliminar",
            java.util.UUID.class,
            java.util.UUID.class
        );
    }

    @Test
    void losDatosDeCatalogoConStockNoSeCachean() throws NoSuchMethodException {
        assertFalse(CatalogoPublicoService.class.getMethod("obtener", java.util.UUID.class)
            .isAnnotationPresent(Cacheable.class));
        assertFalse(CatalogoPublicoService.class.getMethod("obtenerDetalle", java.util.UUID.class)
            .isAnnotationPresent(Cacheable.class));
        assertTrue(CatalogoPublicoService.class.getMethod("listarGaleria", java.util.UUID.class)
            .isAnnotationPresent(Cacheable.class));
    }

    private void assertAllEntriesEvicted(
        Class<?> service,
        String methodName,
        Class<?> parameter,
        String expectedCache
    ) throws NoSuchMethodException {
        assertAllEntriesEvicted(service, methodName, new Class<?>[] {parameter}, expectedCache);
    }

    private void assertAllEntriesEvicted(
        Class<?> service,
        String methodName,
        Class<?> parameter1,
        Class<?> parameter2,
        String expectedCache
    ) throws NoSuchMethodException {
        assertAllEntriesEvicted(service, methodName, new Class<?>[] {parameter1, parameter2}, expectedCache);
    }

    private void assertAllEntriesEvicted(
        Class<?> service,
        String methodName,
        Class<?>[] parameters,
        String expectedCache
    ) throws NoSuchMethodException {
        CacheEvict eviction = service.getMethod(methodName, parameters).getAnnotation(CacheEvict.class);
        assertNotNull(eviction);
        assertTrue(eviction.beforeInvocation());
        assertTrue(eviction.allEntries());
        assertTrue(java.util.List.of(eviction.value()).contains(expectedCache));
    }

    private void assertProductGalleryEvicted(
        Class<?> service,
        String methodName,
        Class<?>... parameters
    ) throws NoSuchMethodException {
        Method method = service.getMethod(methodName, parameters);
        CacheEvict eviction = method.getAnnotation(CacheEvict.class);
        assertNotNull(eviction);
        assertTrue(eviction.beforeInvocation());
        assertTrue(java.util.List.of(eviction.value()).contains("catalogo-galeria"));
        assertFalse(eviction.allEntries());
        assertTrue(eviction.key().contains("productoId"));
    }
}
