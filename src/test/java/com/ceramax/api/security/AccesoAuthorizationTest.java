package com.ceramax.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.ceramax.api.dto.request.ClienteRequest;
import com.ceramax.api.dto.request.ActualizarDatosEntregaClienteRequest;
import com.ceramax.api.dto.request.BuscarClienteRegistroRequest;
import com.ceramax.api.dto.request.UsuarioRequest;
import com.ceramax.api.dto.response.ClienteResponse;
import com.ceramax.api.service.catalogo.ProductoService;
import com.ceramax.api.service.venta.CuponService;
import com.ceramax.api.service.acceso.ClienteService;
import com.ceramax.api.service.acceso.UsuarioService;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AccesoAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private CuponService cuponService;

    @Test
    @WithMockUser(roles = "JEFE_LOGISTICA")
    void jefeDeLogisticaPuedeAdministrarCupones() throws Exception {
        when(cuponService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cupones")
                .with(user("jefe").roles("JEFE_LOGISTICA")))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LOGISTICA")
    void logisticaNoPuedeAdministrarCupones() throws Exception {
        mockMvc.perform(get("/api/v1/cupones"))
            .andExpect(status().isForbidden());
    }

    @Test
    void anunciosDeCuponesSonPublicos() throws Exception {
        mockMvc.perform(get("/api/v1/public/cupones/anuncios"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LOGISTICA")
    void logisticaPuedeConsultarProductosParaRegistrarEntradasPeroNoAdministrarlos() throws Exception {
        when(productoService.listarPagina(any())).thenReturn(
            new PageImpl<>(List.of(), PageRequest.of(0, 20), 0)
        );

        mockMvc.perform(get("/api/v1/catalogo/productos")
                .with(user("logistica").roles("LOGISTICA")))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/catalogo/productos")
                .with(user("logistica").roles("LOGISTICA"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "codigo": "PRO-TEST",
                      "categoriaId": "%s",
                      "nombre": "Producto de prueba",
                      "descripcion": "",
                      "especificaciones": [],
                      "precio": 10.00,
                      "descuentoPorcentaje": 0,
                      "activo": true
                    }
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isForbidden());

        verify(productoService, never()).crear(any());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeAdministrarUsuariosRolesSucursalesNiLeerClientes() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/sucursales")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/clientes")).andExpect(status().isForbidden());

        verify(usuarioService, never()).listar();
        verify(clienteService, never()).listar();
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeCrearPersonalNiAsignarRoles() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "codigo": "USR-TEST",
                      "rolId": "%s",
                      "tipoDocumento": "DNI",
                      "numeroDocumento": "12345678",
                      "nombres": "Test",
                      "apellidos": "Cliente",
                      "email": "test@example.test",
                      "password": "Abcdef908172!",
                      "telefono": "999123456",
                      "departamento": "Lima",
                      "provincia": "Lima",
                      "distrito": "Lima",
                      "direccion": "Jr. Ejemplo 123",
                      "codigoPostal": "15001",
                      "referencia": "Frente al parque",
                      "idUbigeo": "150101"
                    }
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isForbidden());

        verify(usuarioService, never()).crear(any(UsuarioRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void personalNoPuedeSimularPagoComoCliente() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos/web/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoEntrega": "RECOJO_TIENDA",
                      "tipoComprobante": "BOLETA",
                      "metodoPago": "SIMULADO",
                      "detalles": [
                        {
                          "productoId": "%s",
                          "cantidad": 1
                        }
                      ]
                    }
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isForbidden());
    }

    @Test
    void permiteRegistroPublicoDeClienteSinHacerPublicasLasRutasDeConsulta() throws Exception {
        when(clienteService.registrar(any(ClienteRequest.class))).thenReturn(
            new ClienteResponse(
                UUID.randomUUID(),
                "CLI-TEST",
                "Test",
                "Cliente",
                "test@example.test",
                "DNI",
                "12345678",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null
            )
        );

        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoDocumento": "DNI",
                      "numeroDocumento": "12345678",
                      "nombres": "Test",
                      "apellidos": "Cliente",
                      "email": "test@example.test",
                      "password": "Abcdef908172!",
                      "telefono": "999123456",
                      "departamento": "Lima",
                      "provincia": "Lima",
                      "distrito": "Lima",
                      "direccion": "Jr. Ejemplo 123",
                      "codigoPostal": "15001",
                      "referencia": "Frente al parque",
                      "idUbigeo": "150101"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clientes")).andExpect(status().isForbidden());
        verify(clienteService).registrar(any(ClienteRequest.class));
    }

    @Test
    void permiteBuscarDatosDeRegistroPorDocumentoSinToken() throws Exception {
        when(clienteService.buscarDatosParaRegistro(any(BuscarClienteRegistroRequest.class)))
            .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/clientes/registro/buscar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoDocumento": "DNI",
                      "numeroDocumento": "70903432"
                    }
                    """))
            .andExpect(status().isNoContent());

        verify(clienteService).buscarDatosParaRegistro(any(BuscarClienteRegistroRequest.class));
    }

    @Test
    @WithMockUser(username = "cliente@example.test", roles = "CLIENTE")
    void clientePuedeActualizarSoloSusDatosDeEntrega() throws Exception {
        when(clienteService.actualizarMisDatosEntrega(any(ActualizarDatosEntregaClienteRequest.class)))
            .thenReturn(new ClienteResponse(
                UUID.randomUUID(), "CLI-TEST", "Test", "Cliente", "cliente@example.test",
                "DNI", "12345678", "987654321", "Lima", "Lima", "Barranco",
                "Jr. Ejemplo 123", "15001", "Cerca del parque", "150104", true, null, null
            ));

        mockMvc.perform(patch("/api/v1/clientes/mio/datos-entrega")
                .with(user("cliente@example.test").roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "telefono": "987654321",
                      "departamento": "Lima",
                      "provincia": "Lima",
                      "distrito": "Barranco",
                      "direccion": "Jr. Ejemplo 123",
                      "codigoPostal": "15001",
                      "referencia": "Cerca del parque",
                      "idUbigeo": "150104"
                    }
                    """))
            .andExpect(status().isOk());

        verify(clienteService).actualizarMisDatosEntrega(any(ActualizarDatosEntregaClienteRequest.class));
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void personalNoPuedeActualizarElPerfilDeEntregaDeUnCliente() throws Exception {
        mockMvc.perform(patch("/api/v1/clientes/mio/datos-entrega")
                .with(user("vendedor").roles("VENDEDOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "telefono": "987654321",
                      "departamento": "Lima",
                      "provincia": "Lima",
                      "distrito": "Barranco",
                      "direccion": "Jr. Ejemplo 123"
                    }
                    """))
            .andExpect(status().isForbidden());

        verify(clienteService, never())
            .actualizarMisDatosEntrega(any(ActualizarDatosEntregaClienteRequest.class));
    }

    @Test
    void permiteCorsiDesdeLosPuertosLocalesDelFrontend() throws Exception {
        mockMvc.perform(options("/api/v1/public/catalogo")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/api/v1/public/catalogo")
                .header("Origin", "http://localhost:5174")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"));

        mockMvc.perform(options("/api/v1/public/catalogo")
                .header("Origin", "http://localhost:5175")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
