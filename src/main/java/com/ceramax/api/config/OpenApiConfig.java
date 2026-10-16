package com.ceramax.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ceramaxOpenAPI() {
        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("Cerámax API")
                .description("API REST de Cerámax. Las rutas protegidas requieren un JWT de tipo Bearer.")
                .version("v1"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer documentOperations() {
        return openApi -> {
            openApi.setTags(List.of(
                new Tag().name("Autenticación").description("Inicio de sesión para personal y clientes."),
                new Tag().name("Clientes").description("Registro, perfil propio y administración de clientes."),
                new Tag().name("Usuarios").description("Administración del personal del sistema."),
                new Tag().name("Roles").description("Consulta y administración de roles."),
                new Tag().name("Sucursales").description("Consulta y administración de sucursales."),
                new Tag().name("Catálogo público").description("Consulta pública de productos, reseñas y ubicación."),
                new Tag().name("Catálogo interno").description("Administración de categorías, productos e imágenes."),
                new Tag().name("Inventario").description("Stock, movimientos, ajustes, entradas, traslados y almacenes."),
                new Tag().name("Pedidos").description("Ventas, pedidos, entrega, historial y comprobantes."),
                new Tag().name("Tienda del cliente").description("Carrito y lista de deseos del cliente autenticado."),
                new Tag().name("Reseñas").description("Reseñas de clientes y consulta pública."),
                new Tag().name("Chats").description("Soporte y mensajería interna."),
                new Tag().name("Reportes").description("Consulta y exportación de reportes.")
            ));
            openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    if (!isPublicEndpoint(path, method.name())) {
                        operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                    }
                    operation.setTags(List.of(tagFor(path)));
                })
            );
        };
    }

    private String tagFor(String path) {
        if (path.startsWith("/api/auth/")) return "Autenticación";
        if (path.equals("/api/v1/clientes") || path.startsWith("/api/v1/clientes/")) return "Clientes";
        if (path.startsWith("/api/v1/usuarios")) return "Usuarios";
        if (path.startsWith("/api/v1/roles")) return "Roles";
        if (path.startsWith("/api/v1/sucursales")) return "Sucursales";
        if (path.contains("/resenas")) return "Reseñas";
        if (path.startsWith("/api/v1/public/catalogo/") || path.equals("/api/v1/public/catalogo")) {
            return "Catálogo público";
        }
        if (path.startsWith("/api/v1/public/")) return "Catálogo público";
        if (path.startsWith("/api/v1/catalogo/")) return "Catálogo interno";
        if (path.startsWith("/api/v1/inventario/almacenes")) return "Inventario";
        if (path.startsWith("/api/v1/inventario")) return "Inventario";
        if (path.startsWith("/api/v1/pedidos")) return "Pedidos";
        if (path.startsWith("/api/v1/cliente/")) return "Tienda del cliente";
        if (path.startsWith("/api/v1/chats")) return "Chats";
        if (path.startsWith("/api/v1/reportes")) return "Reportes";
        return "API";
    }

    private boolean isPublicEndpoint(String path, String method) {
        return path.startsWith("/api/auth/")
            || path.startsWith("/api/v1/public/")
            || path.startsWith("/api/v1/clientes/registro/")
            || ("/api/v1/clientes".equals(path) && "POST".equals(method))
            || path.equals("/actuator/health")
            || path.startsWith("/actuator/health/");
    }
}
