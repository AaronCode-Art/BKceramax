package com.ceramax.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ceramax.api.dto.request.ClienteRequest;
import com.ceramax.api.dto.request.UsuarioRequest;
import com.ceramax.api.dto.response.ClienteResponse;
import com.ceramax.api.service.acceso.ClienteService;
import com.ceramax.api.service.acceso.UsuarioService;
import java.util.UUID;
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
                      "password": "password",
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
                      "password": "password",
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
