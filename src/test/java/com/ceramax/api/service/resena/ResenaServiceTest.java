package com.ceramax.api.service.resena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.request.CrearResenaRequest;
import com.ceramax.api.dto.request.ResenaImagenRequest;
import com.ceramax.api.dto.response.ResenaResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ConflictException;
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
import com.ceramax.api.repository.venta.PedidoRepository;
import com.ceramax.api.service.catalogo.CloudinaryStorageService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ResenaServiceTest {

    private final ResenaRepository resenaRepository = org.mockito.Mockito.mock(ResenaRepository.class);
    private final ResenaImagenRepository imagenRepository = org.mockito.Mockito.mock(ResenaImagenRepository.class);
    private final PedidoRepository pedidoRepository = org.mockito.Mockito.mock(PedidoRepository.class);
    private final ClienteRepository clienteRepository = org.mockito.Mockito.mock(ClienteRepository.class);
    private final ProductoRepository productoRepository = org.mockito.Mockito.mock(ProductoRepository.class);
    private final EntityManager entityManager = org.mockito.Mockito.mock(EntityManager.class);
    private final CloudinaryStorageService cloudinaryStorageService =
        org.mockito.Mockito.mock(CloudinaryStorageService.class);
    private final UUID clienteId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();
    private final UUID pedidoId = UUID.randomUUID();
    private Cliente cliente;
    private Producto producto;
    private ResenaService service;

    @BeforeEach
    void setUp() {
        service = new ResenaService(
            resenaRepository,
            imagenRepository,
            pedidoRepository,
            clienteRepository,
            productoRepository,
            entityManager,
            cloudinaryStorageService,
            new ResenaMapper()
        );
        cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setEmail("cliente@example.com");
        cliente.setNombres("Ana");
        cliente.setApellidos("Ceramista");
        cliente.setActivo(true);
        producto = new Producto();
        producto.setId(productoId);
        producto.setCodigo("PRO-0001");
        producto.setNombre("Maceta");
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "cliente@example.com",
                null,
                List.of()
            )
        );
        when(clienteRepository.findByEmail("cliente@example.com")).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(productoId)).thenReturn(Optional.of(producto));
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creaResenaSoloConCompraCompletadaYGuardaImagenes() {
        UUID resenaId = UUID.randomUUID();
        ResenaImagenRequest imagenRequest = new ResenaImagenRequest(
            "https://res.cloudinary.com/ceramax/image/upload/reviews/foto.jpg",
            "reviews/foto"
        );
        when(pedidoRepository.existeCompraCompletadaConProducto(pedidoId, clienteId, productoId))
            .thenReturn(true);
        when(resenaRepository.findByIdClienteAndIdProducto(clienteId, productoId)).thenReturn(Optional.empty());
        when(resenaRepository.saveAndFlush(any(Resena.class))).thenAnswer(invocation -> {
            Resena resena = invocation.getArgument(0);
            resena.setId(resenaId);
            return resena;
        });
        ResenaImagen imagen = new ResenaImagen();
        imagen.setId(UUID.randomUUID());
        imagen.setIdResena(resenaId);
        imagen.setUrl(imagenRequest.url());
        imagen.setOrden((short) 0);
        when(imagenRepository.findByIdResenaOrderByOrdenAsc(resenaId)).thenReturn(List.of(imagen));

        ResenaResponse response = service.crear(new CrearResenaRequest(
            productoId,
            pedidoId,
            (short) 5,
            "  Excelente calidad  ",
            List.of(imagenRequest)
        ));

        assertEquals(resenaId, response.id());
        assertEquals("Excelente calidad", response.comentario());
        assertEquals(1, response.imagenes().size());
        assertEquals(imagenRequest.url(), response.imagenes().get(0).url());
        verify(imagenRepository).saveAllAndFlush(any());
    }

    @Test
    void cargaImagenDeResenaEnLaCarpetaDeCloudinaryYLaAsocia() {
        UUID resenaId = UUID.randomUUID();
        MockMultipartFile archivo = new MockMultipartFile(
            "imagenes",
            "foto.jpg",
            "image/jpeg",
            new byte[] {1, 2, 3}
        );
        when(pedidoRepository.existeCompraCompletadaConProducto(pedidoId, clienteId, productoId))
            .thenReturn(true);
        when(productoRepository.existsById(productoId)).thenReturn(true);
        when(cloudinaryStorageService.reviewImageFolder()).thenReturn("ceramax/resenas/imagenes");
        when(cloudinaryStorageService.upload(archivo, "ceramax/resenas/imagenes", null))
            .thenReturn(new CloudinaryStorageService.CloudinaryAsset(
                "https://res.cloudinary.com/ceramax/image/upload/ceramax/resenas/imagenes/foto.jpg",
                "ceramax/resenas/imagenes/foto"
            ));
        when(resenaRepository.findByIdClienteAndIdProducto(clienteId, productoId)).thenReturn(Optional.empty());
        when(resenaRepository.saveAndFlush(any(Resena.class))).thenAnswer(invocation -> {
            Resena resena = invocation.getArgument(0);
            resena.setId(resenaId);
            return resena;
        });
        when(imagenRepository.findByIdResenaOrderByOrdenAsc(resenaId)).thenAnswer(invocation -> {
            ResenaImagen imagen = new ResenaImagen();
            imagen.setId(UUID.randomUUID());
            imagen.setIdResena(resenaId);
            imagen.setUrl("https://res.cloudinary.com/ceramax/image/upload/ceramax/resenas/imagenes/foto.jpg");
            imagen.setPublicId("ceramax/resenas/imagenes/foto");
            imagen.setOrden((short) 0);
            return List.of(imagen);
        });

        ResenaResponse response = service.crearConImagenes(
            new CrearResenaRequest(productoId, pedidoId, (short) 5, "Muy buena pieza", List.of()),
            List.of(archivo)
        );

        assertEquals(1, response.imagenes().size());
        verify(cloudinaryStorageService).upload(archivo, "ceramax/resenas/imagenes", null);
        verify(imagenRepository).saveAllAndFlush(any());
    }

    @Test
    void rechazaResenaDeProductoQueNoEstaEnPedidoCompletado() {
        when(pedidoRepository.existeCompraCompletadaConProducto(pedidoId, clienteId, productoId))
            .thenReturn(false);

        assertThrows(BusinessException.class, () -> service.crear(new CrearResenaRequest(
            productoId, pedidoId, (short) 4, "Comentario", List.of()
        )));

        verify(resenaRepository, never()).saveAndFlush(any(Resena.class));
    }

    @Test
    void rechazaUrlDeImagenQueNoUseHttps() {
        when(pedidoRepository.existeCompraCompletadaConProducto(pedidoId, clienteId, productoId))
            .thenReturn(true);
        when(resenaRepository.findByIdClienteAndIdProducto(clienteId, productoId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.crear(new CrearResenaRequest(
            productoId,
            pedidoId,
            (short) 4,
            "Comentario",
            List.of(new ResenaImagenRequest("javascript:alert(1)", null))
        )));

        verify(resenaRepository, never()).saveAndFlush(any(Resena.class));
    }

    @Test
    void noPermiteCrearOtraResenaActivaParaElMismoProducto() {
        Resena existente = new Resena();
        existente.setId(UUID.randomUUID());
        existente.setActivo(true);
        when(pedidoRepository.existeCompraCompletadaConProducto(pedidoId, clienteId, productoId))
            .thenReturn(true);
        when(resenaRepository.findByIdClienteAndIdProducto(clienteId, productoId))
            .thenReturn(Optional.of(existente));

        assertThrows(ConflictException.class, () -> service.crear(new CrearResenaRequest(
            productoId, pedidoId, (short) 4, "Comentario", List.of()
        )));

        verify(resenaRepository, never()).saveAndFlush(any(Resena.class));
    }

    @Test
    void listaResenasPublicasConFechaInstantConvertidaAUtc() {
        UUID resenaId = UUID.randomUUID();
        Instant creadoEn = Instant.parse("2026-10-16T08:00:00Z");
        ResenaPublicaProjection projection = org.mockito.Mockito.mock(ResenaPublicaProjection.class);
        when(projection.getId()).thenReturn(resenaId);
        when(projection.getProductoId()).thenReturn(productoId);
        when(projection.getProductoCodigo()).thenReturn(producto.getCodigo());
        when(projection.getProductoNombre()).thenReturn(producto.getNombre());
        when(projection.getClienteId()).thenReturn(clienteId);
        when(projection.getClienteNombre()).thenReturn("Ana Ceramista");
        when(projection.getPedidoId()).thenReturn(pedidoId);
        when(projection.getCalificacion()).thenReturn((short) 5);
        when(projection.getComentario()).thenReturn("Muy buena pieza");
        when(projection.getCreadoEn()).thenReturn(creadoEn);
        when(productoRepository.existsById(productoId)).thenReturn(true);
        when(resenaRepository.listarPublicas(productoId, PageRequest.of(0, 50)))
            .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 50), 1));

        ResenaResponse response = service.listarPublicas(productoId, PageRequest.of(0, 50)).getContent().get(0);

        assertEquals(creadoEn.atOffset(ZoneOffset.UTC), response.creadoEn());
        assertEquals(resenaId, response.id());
    }
}
